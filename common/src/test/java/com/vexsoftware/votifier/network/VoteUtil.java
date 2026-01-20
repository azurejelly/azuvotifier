package com.vexsoftware.votifier.network;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.plugin.TestVotifierPlugin;
import com.vexsoftware.votifier.util.CryptoUtil;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class VoteUtil {

    public static byte[] encodePOJOv1(Vote vote) throws Exception {
        return encodePOJOv1(vote, TestVotifierPlugin.getI().getProtocolV1Key().getPublic());
    }

    public static byte[] encodePOJOv1(Vote vote, PublicKey key) throws Exception {
        List<String> ordered = new ArrayList<>();
        ordered.add("VOTE");
        ordered.add(vote.getServiceName());
        ordered.add(vote.getUsername());
        ordered.add(vote.getAddress());
        ordered.add(vote.getTimestamp());

        StringBuilder builder = new StringBuilder();
        for (String s : ordered) {
            builder.append(s).append('\n'); // naive join needed!
        }

        return CryptoUtil.encrypt(builder.toString().getBytes(StandardCharsets.US_ASCII), key);
    }
}
