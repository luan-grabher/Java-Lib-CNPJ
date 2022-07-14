package CNPJ;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;

public class CNPJ {

    private static final String cnpjInfoUrl = "http://cnpj.info/";
    private static final String backupPath = System.getProperty("user.home") + "\\Desktop\\CNPJs.backup";
    private static Map<String, Map<String, String>> backup = new HashMap<>();

    /**
     * Retorna mapa de informações do CNPJ do site cnpj.info
     *
     * @param cnpj CNPJ da empresa, serao considerados somente os numeros com
     * regex.
     * @return Retorna null caso ocorra um erro ou o cnpj não tenha 14
     * caracteres.
     */
    public static Map<String, String> get(String cnpj) {
        try {            
            String cnpjOnlyNumbers = cnpj.replaceAll("[^0-9]+", "");

            if (cnpjOnlyNumbers.length() == 14) {
                //carrega backup
                loadBackup();

                //Se o CNPJ ja estiver no backup e o 'Updated'(Time do getTime do Calendar) for menor que 3 dias, retorna o backup 
                if (backup.containsKey(cnpjOnlyNumbers) && (Calendar.getInstance().getTimeInMillis() - Long.parseLong(backup.get(cnpjOnlyNumbers).get("updated")) < 2592000000L)) {
                    return backup.get(cnpjOnlyNumbers);
                }

                //Espera 6 segundos porque caso fiquem chamando o get vai ficar 10 por minuto
                Thread.sleep(6000);

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
                    String addressHTML = doc.getElementById("content").html();
                    addressHTML = addressHTML.split(".*Endere.o")[1].split("Contatos.*")[0];
                    addressHTML = addressHTML.replaceAll("<h3>", "").replaceAll("</h3>", "");
                    
                    List<String> address = new ArrayList<>(Arrays.asList(addressHTML.split("\n<br>")));
                    
                    if(address.size() == 5){
                        address.remove(1);
                    }
                    
                    infos.put("CNPJ", cnpjOnlyNumbers.toString());
                    infos.put("Rua", address.get(0).split(", ")[0]);
                    infos.put("Rua numero", address.get(0).split(", ")[1]);
                    infos.put("Bairro", address.get(1));
                    infos.put("Cidade", removerAcentos(address.get(2).split(" - ")[0]).toUpperCase());
                    infos.put("UF", address.get(2).split(" - ")[1]);
                    infos.put("CEP", address.get(3).replaceAll("\n", ""));
                    infos.put("updated", String.valueOf(Calendar.getInstance().getTimeInMillis()));
                    
                    //Salva no backup
                    save(infos);

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


    /*
     * Save cnpj to file
     */
    public static Boolean save(Map<String, String> infos) {
        try {
            //Insere as infos no mapa de backup usando a chave como o CNPJ
            backup.put(infos.get("CNPJ"), infos);
            //Salva o mapa de backup no arquivo 'CNPJs.backup' no desktop do usuario
            FileOutputStream fos = new FileOutputStream(backupPath);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(backup);
            oos.close();

            return true;
        } catch (Exception e) {
            //e.printStackTrace();
            return false;
        }
    }

    /*
     * Load cnpj from file
     */
    public static Map<String, Map<String, String>> loadBackup() {
        try {
            //Carrega o mapa de backup do arquivo 'CNPJs.backup' no desktop do usuario
            FileInputStream fis = new FileInputStream(backupPath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            backup = (Map<String, Map<String, String>>) ois.readObject();
            ois.close();

            return backup;
        } catch (Exception e) {
            //e.printStackTrace();
            //empty map
            return new HashMap<>();
        }
    }
}
