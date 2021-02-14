package com.sirius.sdk.agent.aries_rfc.feature_0036_issue_credential;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RequestCredentialMessage extends BaseIssueCredentialMessage{
    public RequestCredentialMessage(String message) {
        super(message);
    }

    public JSONObject credRequest() {
        JSONObject request = this.getJSONOBJECTFromJSON("requests~attach");
        if (request == null) {
            JSONArray arr = this.getJSONArrayFromJSON("requests~attach", new JSONArray());
            if (arr.length() > 0) {
                request = arr.getJSONObject(0);
            }
        }

        if (request != null) {
            String base64 = request.getJSONObject("data").getString("base64");
            byte[] decoded = Base64.getDecoder().decode(base64);
            return new JSONObject(new String(decoded));
        }

        return null;
    }

    public static RequestCredentialMessage.Builder<?> builder() {
        return new RequestCredentialMessage.RequestCredentialMessageBuilder();
    }

    public static abstract class Builder<B extends RequestCredentialMessage.Builder<B>> extends BaseIssueCredentialMessage.Builder<B> {
        String comment = null;
        JSONObject credRequest = null;

        B setComment(String comment) {
            this.comment = comment;
            return self();
        }

        B setCredRequest(JSONObject credRequest) {
            this.credRequest = credRequest;
            return self();
        }

        @Override
        protected JSONObject generateJSON() {
            JSONObject jsonObject = super.generateJSON();

            String id = generateId();
            jsonObject.put("@id", id);
            jsonObject.put("@type", ARIES_DOC_URI + "issue-credential/1.0/offer-credential");

            if (comment != null) {
                jsonObject.put("comment", comment);
            }

            if (credRequest != null) {
                JSONObject requestAttach = new JSONObject();
                requestAttach.put("@id", "cred-request-" + id);
                requestAttach.put("mime-type", "application/json");
                JSONObject data = new JSONObject();
                byte[] base64 = Base64.getEncoder().encode(credRequest.toString().getBytes(StandardCharsets.UTF_8));
                data.put("base64", new String(base64));
                requestAttach.put("data", data);
                JSONArray arr = new JSONArray();
                arr.put(requestAttach);
                jsonObject.put("requests~attach", arr);
            }
            return jsonObject;
        }

        public RequestCredentialMessage build() {
            return new RequestCredentialMessage(generateJSON().toString());
        }
    }

    private static class RequestCredentialMessageBuilder extends Builder<RequestCredentialMessage.RequestCredentialMessageBuilder> {
        @Override
        protected RequestCredentialMessage.RequestCredentialMessageBuilder self() {
            return this;
        }
    }
}
