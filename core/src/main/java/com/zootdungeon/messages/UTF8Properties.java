package com.zootdungeon.messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.zootdungeon.ColaDungeon;

public class UTF8Properties {

    private final Map<String, String> props = new HashMap<>();

    public UTF8Properties(String basePath, Languages lang) {
        load(basePath, lang);
    }

    private void load(String basePath, Languages lang) {
        String propsPath = basePath + ".properties";

        // For ENGLISH, only load the base file (root locale)
        // For other languages, load base first, then override with locale-specific file
        loadFromPath(propsPath);

        if (lang != Languages.ENGLISH && lang != null) {
            String langCode = lang.code();
            String localizedPath = basePath + "_" + langCode + ".properties";
            loadFromPath(localizedPath);
        }
    }

    private void loadFromPath(String propsPath) {
        FileHandle fh = Gdx.files.internal(propsPath);
        if (!fh.exists()) return;

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(fh.read(), StandardCharsets.UTF_8), 8192);

        String key = null;
        StringBuilder value = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') {
                    continue;
                }

                if (key != null && startsWithWhitespace(line)) {
                    value.append('\n');
                    value.append(line);
                    continue;
                }

                if (key != null && value.length() > 0) {
                    props.put(key.toLowerCase(Locale.ENGLISH), decodeEscape(value.toString()));
                }

                int eq = line.indexOf('=');
                if (eq < 0) eq = line.indexOf(':');
                if (eq < 0) {
                    key = null;
                    value.setLength(0);
                    continue;
                }
                key = line.substring(0, eq).trim();
                value.setLength(0);
                value.append(line.substring(eq + 1));
            }
            if (key != null && value.length() > 0) {
                props.put(key.toLowerCase(Locale.ENGLISH), decodeEscape(value.toString()));
            }
        } catch (IOException e) {
            ColaDungeon.reportException(e);
        } finally {
            try { reader.close(); } catch (IOException ignored) {}
        }
    }

    private static boolean startsWithWhitespace(String s) {
        if (s.isEmpty()) return false;
        char c = s.charAt(0);
        return c == ' ' || c == '\t' || c == '\f';
    }

    static String decodeEscape(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        int i = 0, len = raw.length();

        while (i < len) {
            char c = raw.charAt(i++);

            if (c == '\\' && i < len) {
                char e = raw.charAt(i++);
                switch (e) {
                    case 'n':  out.append('\n'); break;
                    case 't':  out.append('\t'); break;
                    case 'r':  out.append('\r'); break;
                    case '\\': out.append('\\'); break;
                    case '\'': out.append('\''); break;
                    case '"':  out.append('"');  break;
                    case 'u':
                        if (i + 4 > len) { out.append('u'); break; }
                        try {
                            int code = Integer.parseInt(raw.substring(i, i + 4), 16);
                            out.append((char) code);
                            i += 4;
                        } catch (NumberFormatException nf) {
                            out.append('u');
                        }
                        break;
                    default:
                        out.append(e);
                        break;
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public String getRaw(String key) {
        return props.get(key.toLowerCase(Locale.ENGLISH));
    }
}
