import java.io.IOException;

public class crawler {
    public static void main (String[] str) throws IOException {
        navigation pr = new navigation();
        pr.crawl("https://www.bbc.com/news");
    }
}
