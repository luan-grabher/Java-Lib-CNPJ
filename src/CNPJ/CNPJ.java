package CNPJ;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CNPJ {

    private static final String cnpjInfoUrl = "http://cnpj.info/";

    public static void main(String[] args) {
        get("74873266000185");
    }

    /**
     * Retorna mapa de informações do CNPJ do site cnpj.info
     *
     * @param cnpj CNPJ da empresa, serao considerados somente os numeros com
     * regex.
     * @return REtorna null caso ocorra um erro ou o cnpj não tenha 14
     * caracteres.
     */
    public static Map<String, String> get(String cnpj) {
        try {
            String cnpjOnlyNumbers = cnpj.replaceAll("[^0-9]+", "");

            if (cnpjOnlyNumbers.length() == 14) {
                URL url = new URL(cnpjInfoUrl + cnpjOnlyNumbers);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                //Se o status for 200
                if (con.getResponseCode() == 200) {
                    //Pega Documento para pegar elementos
                    Document doc = Jsoup.parse(getHtmlFromCon(con));
                    Element table = doc.getElementsByTag("table").first();
                    Elements rows = table.getElementsByTag("tr");
                    
                    Map<String, String> infos = new HashMap<>();
                    
                    rows.forEach((row)->{
                        Elements cols = row.getElementsByTag("td");
                        infos.put(cols.first().text(), cols.last().text());
                    });
                    
                    return infos;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
    * Retorna a string com codigo html de uma conexao
    * 
    * @param con Conexao utilizada
    * @return Retorna o html ou sttring em branco em caso de erros
    */
    private static String getHtmlFromCon(HttpURLConnection con) {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
