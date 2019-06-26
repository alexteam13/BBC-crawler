import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class parser {

    private List<String> outlink = new LinkedList<>();
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

    public void parse(String url, HashSet<Integer> lHash, elasticConnector el) throws IOException {
        Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();
        JSONObject jsonObject = new JSONObject();
        Elements links;
        if (doc != null) {   //если получили документ
            System.out.println("Connected to " + url);
            links = doc.select("a[href]");

            String header = doc.select("div[class=story-body]>h1").text();
            String author = doc.select("div[class=story-body]>div[class=byline]").text()
                    .replace("By ", "");
            String pub_date = doc.select("li[class^=mini] > div[class^=date]").text();
            String text = doc.select("div[class^=story-body__inner]").text();
            text = text.replace(doc.select("div[class^=story-body__inner]> figure").text(),"");

            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("header", header);
                builder.field("author", author);
                builder.timeField("pub_date", pub_date);
                builder.field("text", text);
            }
            builder.endObject();

            if (text != ""){
                System.out.println("Writing to Elasticsearch...");
                el.getBulkProcessor().add(new IndexRequest("bbc", "news")
                        .source(builder));
            }


            for (Element l : links) {
                int hash = l.hashCode();
                String g = l.absUrl("href");
                if (!(lHash.contains(hash))
                        &&((g.contains("https://www.bbc.com/news"))||(g.contains("https://www.bbc.co.uk/news/")))
                        &&!(g.contains("#"))) {
                    lHash.add(hash);
                    outlink.add(l.absUrl("href"));
                }
            }

            System.out.println("Found " + outlink.size() + " new links");
        } else {
            System.out.println("Error while connection to URL: " + url);
        }
        System.out.println();
    }

    public List<String> getLinks()
    {
        return this.outlink;
    }
}
