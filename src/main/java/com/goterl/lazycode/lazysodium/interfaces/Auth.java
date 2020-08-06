/*
 * Copyright (c) Terl Tech Ltd • 14/06/19 17:54 • goterl.com
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.goterl.lazycode.lazysodium.interfaces;


import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.utils.BaseChecker;
import com.goterl.lazycode.lazysodium.utils.Key;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public interface Auth {

    int HMACSHA512256_BYTES = 32,
        HMACSHA512256_KEYBYTES = 32,
        HMACSHA256_BYTES = 32,
        HMACSHA256_KEYBYTES = 32,
        HMACSHA512_BYTES = 64,
        HMACSHA512_KEYBYTES = 32,

        BYTES = HMACSHA512256_BYTES,
        KEYBYTES = HMACSHA512256_KEYBYTES;


    class Checker extends BaseChecker {

    }

    interface Native {

        /**
         * Generate an authentication key.
         * @param k Auth key of size {@link #KEYBYTES}.
         */
        void cryptoAuthKeygen(byte[] k);

        /**
         * Computes a tag for the message in, whose length is inLen bytes, and the key k.
         * @param tag Tag of size {@link #BYTES}.
         * @param in A message.
         * @param inLen Message size.
         * @param key The key as generated by {@link #cryptoAuthKeygen(byte[])}.
         * @return True if successful.
         */
        boolean cryptoAuth(byte[] tag, byte[] in, long inLen, byte[] key);

        /**
         * Verifies that the tag stored at h is a
         * valid tag for the message in whose length
         * is inLen bytes, and the key k.
         * @param tag The tag.
         * @param in The message.
         * @param inLen Message bytes.
         * @param key The key as generated by {@link #cryptoAuthKeygen(byte[])}.
         * @return True if successful verification.
         */
        boolean cryptoAuthVerify(byte[] tag, byte[] in, long inLen, byte[] key);


        void cryptoAuthHMACSha256Keygen(byte[] key);

        boolean cryptoAuthHMACSha256(
                byte[] out,
                byte[] in,
                long inLen,
                byte[] k
        );

        boolean cryptoAuthHMACSha256Verify(
                byte[] h,
                byte[] in,
                long inLen,
                byte[] k
        );

        boolean cryptoAuthHMACSha256Init(
                StateHMAC256 state,
                byte[] key,
                int keyLen
        );

        boolean cryptoAuthHMACSha256Update(
                StateHMAC256 state,
                byte[] in,
                long inLen
        );

        boolean cryptoAuthHMACSha256Final(
                StateHMAC256 state,
                byte[] out
        );


        void cryptoAuthHMACSha512Keygen(byte[] key);

        boolean cryptoAuthHMACSha512(
                byte[] out,
                byte[] in,
                long inLen,
                byte[] k
        );

        boolean cryptoAuthHMACSha512Verify(
                byte[] h,
                byte[] in,
                long inLen,
                byte[] k
        );

        boolean cryptoAuthHMACSha512Init(
                StateHMAC512 state,
                byte[] key,
                int keyLen
        );

        boolean cryptoAuthHMACSha512Update(
                StateHMAC512 state,
                byte[] in,
                long inLen
        );

        boolean cryptoAuthHMACSha512Final(
                StateHMAC512 state,
                byte[] out
        );



        void cryptoAuthHMACSha512256Keygen(byte[] key);

        boolean cryptoAuthHMACSha512256(
                byte[] out,
                byte[] in,
                long inLen,
                byte[] k
        );

        boolean cryptoAuthHMACSha512256Verify(
                byte[] h,
                byte[] in,
                long inLen,
                byte[] k
        );

        boolean cryptoAuthHMACSha512256Init(
                StateHMAC512256 state,
                byte[] key,
                int keyLen
        );

        boolean cryptoAuthHMACSha512256Update(
                StateHMAC512256 state,
                byte[] in,
                long inLen
        );

        boolean cryptoAuthHMACSha512256Final(
                StateHMAC512256 state,
                byte[] out
        );

    }

    interface Lazy {

        /**
         * Generate an authentication key.
         * @return An authentication key.
         */
        Key cryptoAuthKeygen();

        /**
         * Computes a tag for the message in.
         * @param message A message.
         * @param key The key as generated by {@link #cryptoAuthKeygen()}.
         * @return True if successful.
         */
        String cryptoAuth(String message, Key key) throws SodiumException;


        /**
         * Verifies that the tag
         * valid tag for the message.
         * @param tag The tag.
         * @param key The key as generated by {@link #cryptoAuthKeygen()}.
         * @return True if successful verification.
         */
        boolean cryptoAuthVerify(String tag, String message, Key key) throws SodiumException;


        Key cryptoAuthHMACShaKeygen(Type type);

        String cryptoAuthHMACSha(Type type, String in, Key key);

        boolean cryptoAuthHMACShaVerify(
                Type type,
                String authenticator,
                String message,
                Key key
        );

        boolean cryptoAuthHMACShaInit(
                StateHMAC256 state,
                Key key
        );

        boolean cryptoAuthHMACShaUpdate(
                StateHMAC256 state,
                String in
        );

        String cryptoAuthHMACShaFinal(
                StateHMAC256 state
        ) throws SodiumException;

        boolean cryptoAuthHMACShaInit(
                StateHMAC512 state,
                Key key
        );

        boolean cryptoAuthHMACShaUpdate(
                StateHMAC512 state,
                String in
        );

        String cryptoAuthHMACShaFinal(
                StateHMAC512 state
        ) throws SodiumException;



        boolean cryptoAuthHMACShaInit(
                StateHMAC512256 state,
                Key key
        );

        boolean cryptoAuthHMACShaUpdate(
                StateHMAC512256 state,
                String in
        );

        String cryptoAuthHMACShaFinal(
                StateHMAC512256 state
        ) throws SodiumException;



    }

    enum Type {
        SHA256,
        SHA512,
        SHA512256
    }

    class StateHMAC256 extends Structure {
        public Hash.State256 ictx;
        public Hash.State256 octx;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("ictx", "octx");
        }
    }

    class StateHMAC512 extends Structure {
        public Hash.State512 ictx;
        public Hash.State512 octx;


        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("ictx", "octx");
        }
    }

    class StateHMAC512256 extends Structure {
        public Hash.State512 ictx;
        public Hash.State512 octx;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("ictx", "octx");
        }
    }


}