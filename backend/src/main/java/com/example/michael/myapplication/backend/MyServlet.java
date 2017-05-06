/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.example.michael.myapplication.backend;

import com.algorithmia.AlgorithmException;
import com.algorithmia.Algorithmia;
import com.algorithmia.AlgorithmiaClient;
import com.algorithmia.algo.AlgoResponse;
import com.algorithmia.algo.Algorithm;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.http.*;

import jdk.nashorn.internal.parser.JSONParser;


public class MyServlet extends HttpServlet {
    private AlgorithmiaClient client= Algorithmia.client("simMbgATg/yyhF6aiFnC8W2Hm1m1");
    private Algorithm algo = client.algo("algo://deeplearning/ColorfulImageColorization/1.1.6");
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Please use the form to POST to this url");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String name = req.getParameter("name");
        String inputUrl = "https://storage.googleapis.com/automaticimagecolorization.appspot.com/images/" + name;
        URL url = new URL("https://api.algorithmia.com/v1/algo/deeplearning/ColorfulImageColorization/1.1.6");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Simple simMbgATg/yyhF6aiFnC8W2Hm1m1");
        conn.setDoInput(true);
        conn.setReadTimeout(100000);

        //String headers = "{'Content-Type': 'application/json', 'Authorization': 'Simple simMbgATg/yyhF6aiFnC8W2Hm1m1'}";
        //byte[] outputBytes = ("{'image':'" + inputUrl + "'}").getBytes("UTF-8");
        JSONObject jsonObj = new JSONObject().put("image", inputUrl);
        //JSONObject jsonObject = new JSONObject(input);
        OutputStream os = conn.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");

        osw.write(jsonObj.toString());
        osw.flush();
        osw.close();
        conn.connect(); // Note the connect() here
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader( conn.getInputStream(),"utf-8"));
        String lines = null;
        while ((lines = br.readLine()) != null) {
            sb.append(lines + "\n");
        }
        br.close();
        System.out.println(""+sb.toString());
        JSONObject jsonObject = new JSONObject(sb.toString());
        String colorUrl = jsonObject.getJSONObject("result").getString("output");
        resp.setContentType("text/plain");
        resp.getWriter().println(colorUrl);
        /*

        int respCode = conn.getResponseCode();  // New items get NOT_FOUND on PUT
        if (respCode == HttpURLConnection.HTTP_OK || respCode == HttpURLConnection.HTTP_NOT_FOUND) {
            req.setAttribute("error", "");
            StringBuffer response = new StringBuffer();
            String line;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            req.setAttribute("response", response.toString());
        } else {
            req.setAttribute("error", conn.getResponseCode() + " " + conn.getResponseMessage());
        }

        */
    }


}
