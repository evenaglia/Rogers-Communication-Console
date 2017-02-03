/*
 * Copyright 2016 - 2017 Ed Venaglia
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.venaglia.roger.emoji;

import com.google.inject.Singleton;
import com.venaglia.roger.bundle.AbstractLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ed on 9/23/16.
 */
@Singleton
public class EmojiSetLoader extends AbstractLoader<EmojiSet> {

    private static final String source = "emoji.json";

    @Override
    protected EmojiSet load() {
        Map<String,Emoji> emojiMap = new HashMap<>();
        Reader reader = new InputStreamReader(getStream(source), StandardCharsets.UTF_8);
        String s = readString(reader);
        s = s.replaceAll("(?m)^\\s*//.*$", "");
        JSONObject obj = new JSONObject(s);
        JSONArray array = obj.getJSONArray("emoji");
        for (int i = 0, l = array.length(); i < l; i++) {
            Emoji emoji = parseEmoji(array.getJSONObject(i));
            emojiMap.put(emoji.getId(), emoji);
        }
        return new EmojiSet() {
            @Override
            public Emoji get(String id) {
                return emojiMap.get(id);
            }

            @Override
            public Emoji[] getMany(String... ids) {
                Emoji[] emojis = new Emoji[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    emojis[i] = emojiMap.get(ids[i]);
                }
                return emojis;
            }
        };
    }

    private Emoji parseEmoji(JSONObject json) {
        return new Emoji(json.getString("id"),
                         bufferImage(json.optString("image", null)),
                         json.optString("text"));
    }
}
