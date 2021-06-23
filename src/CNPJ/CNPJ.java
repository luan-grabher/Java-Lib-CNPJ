package CNPJ;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.Normalizer;
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
                    
                    //Pega endereço
                    String address = doc.getElementById("content").html();
                    address = address.split(".*Endere.o")[1].split("Contatos.*")[0];
                    address = address.replaceAll("<h3>", "").replaceAll("</h3>", "");
                    
                    String[] addressArray = address.split("\n<br>");
                    
                    infos.put("Rua", addressArray[0].split(", ")[0]);
                    infos.put("Rua numero", addressArray[0].split(", ")[1]);
                    infos.put("Bairro", addressArray[1]);
                    infos.put("Cidade", removerAcentos(addressArray[2].split(" - ")[0])).toUpperCase();
                    infos.put("UF", addressArray[2].split(" - ")[1]);
                    infos.put("CEP", addressArray[3].replaceAll("\n", ""));                    
                    
                    return infos;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    
    public static String removerAcentos(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
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
                    new InputStreamReader(con.getInputStream(), Charset.forName("ISO-8859-1")));
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
