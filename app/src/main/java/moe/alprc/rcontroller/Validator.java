package moe.alprc.rcontroller;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Validator {
    private static final String TAG = Validator.class.getSimpleName();
    private static final String TAG_TRIM = "StringTrim";

    private static final String ACCEPT_FILENAME = "accept.txt";
    private static final String SUB_FILENAME = "sub.json";
    private Set<String> accept;
    private Map<String, String> sub;
    private Set<String> subKeySet;
    private int maxLength = 0;

    private Activity activity;

    private boolean inited = false;

    Validator(Activity activity) {
        this.activity = activity;
        if (init(true)) {
            inited = true;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean init(boolean firstCall) {
        InputStream accept_is;
        InputStream sub_is;

        // on first run, check the read permission.
        // request the permission if don't have it already.
        // if not the first time, try loading from external storage, then the internal storage.
        if (firstCall && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            return false;
        }
        try {
            File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File
                    .separator + "RController" + File.separator);
            File externalAccept = new File(root.getAbsolutePath() + File.separator + ACCEPT_FILENAME);
            if (externalAccept.exists()) {
                accept_is = new FileInputStream(externalAccept);
            } else {
                accept_is = activity.getResources().getAssets().open(ACCEPT_FILENAME);
            }
            BufferedReader subReader = new BufferedReader(new InputStreamReader(accept_is));
            accept = new HashSet<>();
            String line;
            while ((line = subReader.readLine()) != null) {
                if (line.length() > 0) {
                    accept.add(line);
                }
            }

            File externalSub = new File(root.getAbsolutePath() + File.separator + SUB_FILENAME);
            if (externalSub.exists()) {
                sub_is = new FileInputStream(externalSub);
            } else {
                sub_is = activity.getResources().getAssets().open(SUB_FILENAME);
            }
            InputStreamReader reader = new InputStreamReader(sub_is);
            sub = new HashMap<>();
            Type type = new TypeToken<HashMap<String, String>>() {
            }.getType();

            Gson gson = new Gson();
            try {
                sub = (HashMap<String, String>) gson.fromJson(reader, type);
            } catch (JsonSyntaxException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            subKeySet = sub.keySet();
            for (String key : subKeySet) {
                maxLength = Math.max(maxLength, key.split(" ").length);
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }


        return true;
    }

    /**
     * trim trims a string, and removes all line breaks.
     *
     * @param str is the String to be trimmed.
     * @return the trimmed string.
     */
    private String trim(String str) {
        if (str == null) {
            str = "";
        }
        Log.i(TAG_TRIM, "Trim(" + str + ")");
        str = str.trim();
        if (str.length() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            char lastChar = str.charAt(0);
            for (int i = 0; i < str.length(); ++i) {
                char ch = str.charAt(i);
                if (ch != '\n' && ch != '\r' && (ch != ' ' || lastChar != ' ')) {
                    stringBuilder.append(ch);
                }
                lastChar = ch;
            }
            str = stringBuilder.toString();
        }

        Log.i(TAG_TRIM, "get \"" + str + "\"");
        return str;
    }

    private void validate(Topic topic, String arg) throws IllegalArgumentException {
        StringBuilder exceptionMessageBuilder = new StringBuilder();
        switch (topic.getTopicName()) {
            case TopicData.SPRFORNLP:
                String[] args = arg.split(" ");

                for (String key : args) {
                    if (!accept.contains(key)) {
                        exceptionMessageBuilder.append("\"").append(key).append("\", ");
                    }
                }

                if (exceptionMessageBuilder.length() > 0) {
                    exceptionMessageBuilder.deleteCharAt(exceptionMessageBuilder.length() - 1);
                    exceptionMessageBuilder.deleteCharAt(exceptionMessageBuilder.length() - 1);
                    throw new IllegalArgumentException(exceptionMessageBuilder.toString());
                }
                break;
            default:
                if (arg.length() == 0) {
                    throw new IllegalArgumentException();
                }
        }
    }

    private List<String> replace(List<String> list, final List<String> subList, String replaceText) {
        for (int i = 0; i < list.size() - subList.size() + 1; ++i) {
            if (list.get(i).equals(subList.get(0))) {
                boolean equals = true;
                for (int ii = 0; ii < subList.size(); ++ii) {
                    if (!list.get(i + ii).equals(subList.get(ii))) {
                        equals = false;
                        break;
                    }
                }
                if (equals) {
                    list.set(i, replaceText);
                    for (int ii = 1; ii < subList.size(); ++ii) {
                        list.remove(i + 1);
                    }
                }
            }
        }
        return list;
    }

    private String listToString(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for (String str : list) {
            if (str != null) {
                builder.append(str).append(" ");
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    private String replace(String arg) {
        List<String> list = new LinkedList<>(Arrays.asList(arg.split(" ")));
        Log.i(TAG, "Max length = " + maxLength);
        for (int o = 2; o <= maxLength; ++o) {
            for (int i = 0; i < list.size() - o + 1; ++i) {
                List<String> subStrList = new LinkedList<>();
                for (int ii = 0; ii < o; ++ii) {
                    subStrList.add(list.get(i + ii));
                }
                String subStr = listToString(subStrList);
                if (subKeySet.contains(subStr)) {
                    list = replace(list, subStrList, sub.get(subStr));
                }
            }
        }
        arg = listToString(list);
        return arg;
    }

    String process(Topic topic, String arg) throws IllegalFormatException {
        if (!inited) {
            init(false);
        }
        arg = trim(arg);
        validate(topic, arg);


        // arg = replace(arg);
        Log.i(TAG, "formatted arg: \"" + arg + "\"");

        return arg;
    }
}
