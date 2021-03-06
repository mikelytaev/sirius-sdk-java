import com.sirius.sdk.agent.Agent;
import com.sirius.sdk.agent.Codec;
import com.sirius.sdk.agent.Event;
import com.sirius.sdk.agent.Ledger;
import com.sirius.sdk.agent.aries_rfc.feature_0037_present_proof.RequestPresentationMessage;
import com.sirius.sdk.agent.aries_rfc.feature_0037_present_proof.StateMachineProver;
import com.sirius.sdk.agent.aries_rfc.feature_0037_present_proof.StateMachineVerifier;
import com.sirius.sdk.agent.model.ledger.CredentialDefinition;
import com.sirius.sdk.agent.model.ledger.Schema;
import com.sirius.sdk.agent.model.pairwise.Pairwise;
import com.sirius.sdk.agent.wallet.abstract_wallet.model.AnonCredSchema;
import com.sirius.sdk.hub.Context;
import com.sirius.sdk.utils.Pair;
import com.sirius.sdk.utils.Triple;
import helpers.ConfTest;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class TestAriesFeature0037 {

    ConfTest confTest;
    Logger log = Logger.getLogger(TestAriesFeature0037.class.getName());

    @Before
    public void configureTest() {
        confTest = ConfTest.newInstance();
    }

    @Test
    public void testSane() throws InterruptedException, ExecutionException, TimeoutException {
        Agent issuer = confTest.getAgent("agent1");
        Agent prover = confTest.getAgent("agent2");
        Agent verifier = confTest.getAgent("agent3");

        issuer.open();
        prover.open();
        verifier.open();

        log.info("Establish pairwises");
        Pairwise i2p = confTest.getPairwise(issuer, prover);
        Pairwise p2i = confTest.getPairwise(prover, issuer);
        Pairwise v2p = confTest.getPairwise(verifier, prover);
        Pairwise p2v = confTest.getPairwise(prover, verifier);

        log.info("Register schema");
        String issuerDid = i2p.getMe().getDid();
        String issuerVerkey = i2p.getMe().getVerkey();
        String schemaName = "schema_" + UUID.randomUUID().toString();
        Pair<String, AnonCredSchema> schemaPair = issuer.getWallet().getAnoncreds().issuerCreateSchema(issuerDid, schemaName, "1.0", "attr1", "attr2", "attr3");
        String schemaId = schemaPair.first;
        AnonCredSchema anoncredSchema = schemaPair.second;
        Ledger ledger = issuer.getLedgers().get("default");
        Pair<Boolean, Schema> okSchema = ledger.registerSchema(anoncredSchema, issuerDid);
        Assert.assertTrue(okSchema.first);
        Schema schema = okSchema.second;

        log.info("Register credential def");
        Pair<Boolean, CredentialDefinition> okCredDef = ledger.registerCredDef(new CredentialDefinition("TAG", schema), issuerDid);
        Assert.assertTrue(okCredDef.first);
        CredentialDefinition credDef = okCredDef.second;

        log.info("Prepare Prover");
        prover.getWallet().getAnoncreds().proverCreateMasterSecret(ConfTest.proverMasterSecretName);

        String proverSecretId = ConfTest.proverMasterSecretName;
        JSONObject credValues = (new JSONObject()).
                put("attr1", "Value-1").
                put("attr2", "456").
                put("attr3", "5.67");

        String credId = "cred-id-" + UUID.randomUUID().toString();

        // Issue credential
        JSONObject offer = issuer.getWallet().getAnoncreds().issuerCreateCredentialOffer(credDef.getId());
        Pair<JSONObject, JSONObject> proverCreateCredentialReqRes = prover.getWallet().getAnoncreds().proverCreateCredentialReq(p2i.getMe().getDid(), offer, new JSONObject(credDef.getBody().toString()), proverSecretId);
        JSONObject credRequest = proverCreateCredentialReqRes.first;
        JSONObject credMetadata = proverCreateCredentialReqRes.second;

        JSONObject encodedCredValues = new JSONObject();
        for (String key : credValues.keySet()) {
            JSONObject encCredVal = new JSONObject();
            encCredVal.put("raw", credValues.get(key).toString());
            encCredVal.put("encoded", Codec.encode(credValues.get(key)));
            encodedCredValues.put(key, encCredVal);
        }
        Triple<JSONObject, String, JSONObject> issuerCreateCredentialRes = issuer.getWallet().getAnoncreds().issuerCreateCredential(offer, credRequest, encodedCredValues);
        JSONObject cred = issuerCreateCredentialRes.first;
        String credRevocId = issuerCreateCredentialRes.second;
        JSONObject revocRegDelta = issuerCreateCredentialRes.third;

        prover.getWallet().getAnoncreds().proverStoreCredential(credId, credMetadata, cred, new JSONObject(credDef.getBody().toString()));


        String attrReferentId = "attr1_referent";
        String predReferentId = "predicate1_referent";

        JSONObject proofRequest = (new JSONObject()).
                put("nonce", verifier.getWallet().getAnoncreds().generateNonce()).
                put("name", "Test ProofRequest").
                put("version", "0.1").
                put("requested_attributes", (new JSONObject()).
                        put("attr_referent_id", (new JSONObject()).
                                put("name", "attr1").
                                put("restrictions", (new JSONObject()).
                                        put("issuer_did", issuerDid)))).
                put("requested_predicates", (new JSONObject()).
                        put("pred_referent_id", (new JSONObject()).
                                put("name", "attr2").
                                put("p_type", ">=").
                                put("p_value", 100).
                                put("restrictions", (new JSONObject()).
                                        put("issuer_did", issuerDid))));

        //run_verifier
        CompletableFuture<Boolean> runVerifier = CompletableFuture.supplyAsync(() -> {
            Ledger verLedger = verifier.getLedgers().get("default");
            Context context = new Context();
            context.agent = verifier;
            StateMachineVerifier machine = new StateMachineVerifier(context, v2p, verLedger);
            StateMachineVerifier.VerifyParams params = new StateMachineVerifier.VerifyParams();
            params.proofRequest = proofRequest;
            params.comment = "I am Verifier";
            params.protoVersion = "1.0";
            return machine.verify(params);
        }, r -> new Thread(r).start());

        //run prover
        CompletableFuture<Boolean> runProver = CompletableFuture.supplyAsync(() -> {
            Event event = null;
            try {
                event = prover.subscribe().getOne().get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                return false;
            }
            Assert.assertTrue(event.message() instanceof RequestPresentationMessage);
            RequestPresentationMessage requestPresentationMessage = (RequestPresentationMessage) event.message();
            int ttl = 60;
            Ledger proverLedger = prover.getLedgers().get("default");
            Context context = new Context();
            context.agent = prover;
            StateMachineProver machine = new StateMachineProver(context, p2v, proverLedger);
            return machine.prove(requestPresentationMessage, proverSecretId);
        }, r -> new Thread(r).start());

        Assert.assertTrue(runProver.get(30, TimeUnit.SECONDS));
        Assert.assertTrue(runVerifier.get(30, TimeUnit.SECONDS));
    }
}