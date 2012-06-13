package wi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.json.JSONObject;


/**
 * The Microsoft Translator API is available through the Windows Azure Marketplace. 
 * We offer a free service usage limit of 2 million characters per month as well as paid monthly 
 * subscriptions for higher volumes.
 * 
 * @author yangjinfeng
 *
 */
public class BingTranslator {
    static HttpClient httpclient = new DefaultHttpClient();
    
    
    
    public static String getToken()throws Exception{
    	
    	String datamarketAccessUri = "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13";
    	HttpPost post = new HttpPost(datamarketAccessUri);
    	List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    	nvps.add(new BasicNameValuePair("grant_type", "client_credentials"));
    	nvps.add(new BasicNameValuePair("client_id", "yangjinfeng"));
    	nvps.add(new BasicNameValuePair("client_secret", "BROx7WorZYCyf4IYRABH86vXzyusd9s14HLV+NPV51Q="));
    	nvps.add(new BasicNameValuePair("scope", "http://api.microsofttranslator.com"));
    	post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

    	HttpResponse response = httpclient.execute(post);
    	BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
    	//		StringBuffer sb = new StringBuffer();
    	String line = null;
    	String json = "";
    	while((line = br.readLine())!= null){
    		json = json + line;
    	}
    	JSONObject js = new JSONObject(json);
    	
    	String token = js.getString("access_token");
    	
    	return token;


    }
    
    /**
     * 只能是get
     * @param fromLanguage : en
     * @param toLanguage : zh-CHS
     * @param text
     * @throws Exception
     */
    public static String translate(String fromLanguage,String toLanguage,String text)throws Exception{

    	String newtext = URLEncoder.encode(text, "UTF-8");
    	String uri = "http://api.microsofttranslator.com/v2/Http.svc/Translate?from="+fromLanguage+"&to="+toLanguage+"&text="+newtext;
    	String token = getToken();
    	String authToken = "Bearer" + " " + token;
    	HttpGet post = new HttpGet(uri);
    	post.addHeader("Authorization", authToken);
    	
    	HttpResponse response = httpclient.execute(post);
    	BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
    	String line = null;
    	String value = "";
    	while((line = br.readLine())!= null){
    		value = value + line;
    	}
    	String translated = extractTranslatedValue(value);
    	return (translated);
    }
    
    
    /**
     * 只能是post
     * An array containing the texts for translation. All strings must be of the same language.
     * The total of all texts to be translated must not exceed 10000 characters. 
     * The maximum number of array elements is 2000.
     * @param fromLanguage
     * @param toLanguage
     * @param text
     * @throws Exception
     */
    public static List<String> translateArray(String fromLanguage,String toLanguage,List<String> texts)throws Exception{
    	String uri = "http://api.microsofttranslator.com/v2/Http.svc/TranslateArray";
    	String token = getToken();
    	String authToken = "Bearer" + " " + token;
    	
    	URL url = new URL(uri);
    	HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    	conn.addRequestProperty("Authorization", authToken);
    	conn.addRequestProperty("Content-Type", "text/xml");
    	conn.setRequestMethod("POST");
    	
    	conn.setDoOutput(true);
    	String body = createBody(fromLanguage,toLanguage,texts);
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(),"utf-8"));		
		pw.print(body);
		pw.close();
		
		int code = conn.getResponseCode();
		InputStream ist = null;
		if(code != 200){
			ist = conn.getErrorStream();
		}else{
			ist = conn.getInputStream();
		}
		if(ist == null){
			ist = conn.getInputStream();
		}
    	BufferedReader br = new BufferedReader(new InputStreamReader(ist, "utf-8"));
    	String line = null;
    	String values = "";
    	while((line = br.readLine())!= null){
    		values = values+line;
    	}
    	return extractTranslatedValues(values);
    }
    
    
    private static String createBody(String fromLanguage,String toLanguage,List<String> texts){
		StringBuffer stringbuffer = new StringBuffer();
		String body = "<TranslateArrayRequest>" +
							"<AppId />" +
							"<From>"+fromLanguage+"</From>" +
							"<Options>" +
								" <Category xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
								"<ContentType xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\">text/plain</ContentType>" +
								"<ReservedFlags xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
								"<State xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
								"<Uri xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
								"<User xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
							"</Options>" ;

		stringbuffer.append(body);
		stringbuffer.append("<Texts>");
		for(String text : texts){
			stringbuffer.append("<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">"+text+"</string>");
		}
		stringbuffer.append("</Texts>");
		stringbuffer.append( "<To>"+toLanguage+"</To></TranslateArrayRequest>");
		return stringbuffer.toString();

    }
    
    
    private static String extractTranslatedValue(String translatedText){
    	int startIndex = translatedText.indexOf(">") + 1;
    	int endIndex = translatedText.lastIndexOf("<");
    	return translatedText.substring(startIndex,endIndex);
    }
    
    private static List<String> extractTranslatedValues(String translatedText)throws Exception{
    	List<String> result = new ArrayList<String>();
    	StringReader reader = new StringReader(translatedText);
    	SAXBuilder sb = new SAXBuilder();
    	Document doc = sb.build(reader);
    	Element root = doc.getRootElement(); 
    	List<Element> values = root.getChildren(); 
    	Namespace ns = Namespace.getNamespace("http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2");
    	for(Element v : values){
    		result.add(v.getChildText("TranslatedText",ns));
    	}
    	return result;
    }
    
    
    public static void main(String[] args) throws Exception{
    	String inutFile = args[0];
    	String outFile = args[1];
    	BufferedReader br = new BufferedReader(new FileReader(inutFile));
    	String line = null;
    	List<String> texts = new ArrayList<String>();
    	while((line = br.readLine())!= null){
    		texts.add(line);
    	}
    	br.close();
    	List<String> results = translateArray("en","zh-CHS",texts);
    	PrintWriter pw = new PrintWriter(outFile,"UTF-8");
    	for(int i = 0;i < texts.size();i ++){
    		pw.println(texts.get(i)+"    "+results.get(i));
    	}
    	pw.close();
    	
    	
	}

}
