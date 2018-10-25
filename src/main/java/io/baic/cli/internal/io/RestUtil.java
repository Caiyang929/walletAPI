package io.baic.cli.internal.io;

import com.google.gson.*;
import io.baic.cli.exception.BaicException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class RestUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestUtil.class);

    private List<Object> providers;
    private GsonBuilder builder;
    private static String encoding = "utf-8";

    private Gson gson;
    private JsonParser parse;

    public RestUtil() {
        builder = new GsonBuilder().disableHtmlEscaping();

        providers = new ArrayList<>();
        providers.add(builder);

        gson = new Gson();
        parse = new JsonParser();

    }

    public JsonElement call(String url, String command, JsonElement request) throws BaicException {
        return call(url, command, request.toString());
    }

    public JsonElement call(String url, String command, String request) throws BaicException {
        try {
            HttpURLConnection httpConn = (HttpURLConnection) (new URL(url + command)).openConnection();
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Accept", "application/json");
            OutputStream outputStream = httpConn.getOutputStream();
            if (request != null) outputStream.write(request.getBytes());
            outputStream.flush();


            // System.out.println(httpConn.getResponseCode());
            int responseCode = httpConn.getResponseCode();

            if (responseCode == 500) {

                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpConn.getErrorStream()));
                String buffer;
                while ((buffer = reader.readLine()) != null) {
                    builder.append(buffer);
                }
                JsonElement element = parse.parse(builder.toString());
                int code = ((JsonObject) element).get("error").getAsJsonObject().get("code").getAsInt();
                String name = ((JsonObject) element).get("error").getAsJsonObject().get("name").getAsString();
                String what = ((JsonObject) element).get("error").getAsJsonObject().get("what").getAsString();
                throw new BaicException(code, name, what);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader((httpConn.getInputStream()))
            );

            StringBuilder builder = new StringBuilder();

            String buffer;
            while ((buffer = reader.readLine()) != null) {
                builder.append(buffer);
            }

            String response = builder.toString();


            boolean debug = true;
            if (debug) {

                System.out.println("REQUEST : " + url + command);
                System.out.println("---------------------");
                if (request != null)
                    System.out.println(request);
            }

            // instread of using post(request, JsonElement.class), post(request) can get
            // the raw common of response
            // we do it in order to get parse the raw common and get the real error message

            if (debug) {
                System.out.println("---------------------");
                System.out.println("RESPONSE");
                System.out.println("---------------------");
                System.out.println(response);
                System.out.println("---------------------");
            }
            /*
            JsonElement element = parse.parse(response);
            if (element.isJsonObject()) {
                // try to get error code
                JsonElement internalErrorObject = ((JsonObject) element).get("code");
                if (internalErrorObject != null) {
                    int errorCode = internalErrorObject.getAsInt();
                    if (errorCode == 500) {
                        int code = ((JsonObject) element).get("error").getAsJsonObject().get("code").getAsInt();
                        String name = ((JsonObject) element).get("error").getAsJsonObject().get("name").getAsString();
                        String what = ((JsonObject) element).get("error").getAsJsonObject().get("what").getAsString();
                        throw new BaicException(code, name, what);
                    }
                }
            }
            */

            return parse.parse(response);
        }
        catch (ProtocolException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
