package com.vexsoftware.votifier.util;

import com.vexsoftware.votifier.model.Vote;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
@UtilityClass
public class ArgsToVote {

    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("([a-zA-Z]+)=(\\S*)");

    public static Vote parse(String[] arguments)  {
        return parse(arguments, null);
    }

    public static Vote parse(String[] arguments, Map<String, String> additionalArgs) {
        long localTimestamp = System.currentTimeMillis();
        String timestamp = Long.toString(localTimestamp, 10);
        String serviceName = "TestVote";
        String username = null;
        String address = "127.0.0.1";

        for (String s : arguments) {
            Matcher m = ARGUMENT_PATTERN.matcher(s);
            if (m.matches()) {
                String key = m.group(1).toLowerCase();
                String value = m.group(2);

                switch (key) {
                    case "servicename": {
                        serviceName = value;
                        break;
                    }
                    case "username": {
                        if (value.length() > 16) {
                            throw new IllegalArgumentException(
                                    "Illegal username - must be less than 16 characters long."
                            );
                        }

                        username = value;
                        break;
                    }
                    case "address":
                        address = value;
                        break;
                    case "timestamp": {
                        timestamp = value;
                        break;
                    }
                    default: {
                        if (additionalArgs != null) {
                            additionalArgs.put(key, value);
                        }

                        break;
                    }
                }

            } else {
                if (s.length() > 16) {
                    throw new IllegalArgumentException("Illegal username - must be less than 16 characters long.");
                }

                username = s;
            }
        }

        if (username == null) {
            throw new IllegalArgumentException("Username not specified!");
        }

        return new Vote(serviceName, username, address, timestamp);
    }
}
