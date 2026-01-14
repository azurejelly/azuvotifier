package com.vexsoftware.votifier.util.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GsonInst {

    public static final Gson GSON = new GsonBuilder().create();
}
