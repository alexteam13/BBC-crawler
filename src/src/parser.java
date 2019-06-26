package src;

import org.apache.logging.log4j.LogManager;                                                         //логгер ошибок
import org.apache.logging.log4j.core.Logger;                                                       //тоже логгер
import org.elasticsearch.action.index.IndexRequest;                                               //библиотека для составления запроса на внесение документа в базу
import org.elasticsearch.common.xcontent.XContentBuilder;                                        //библиотека для создания документа (объекта JSON)
import org.elasticsearch.common.xcontent.XContentFactory;                                       //содержит в себе предыдущую библиотеку
import org.json.JSONObject;                                                                    //для объявления объекта-документа
import org.jsoup.Jsoup;                                                                       //библиотека для подключения и парсинга страниц
import org.jsoup.nodes.Document;                                                             //класс для скачанной страницы
import org.jsoup.nodes.Element;                                                             //класс для хранения одного объекта со скачанной страницы
import org.jsoup.select.Elements;                                                          //класс для хранения группы одинаковых объектов

import java.io.IOException;                                                                  //ошибка при чтении/записи файла, сетевого файла или потока
import java.net.SocketTimeoutException;                                                     //ошибка когда требуется слишком много времени, чтобы получить ответ от другого устройства,
                                                                                            // и ваш запрос истекает, прежде чем получить ответ

import java.util.HashSet;                                                                  //множество с организацией поиска по хэш-таблице
import java.util.LinkedList;                                                                //двунаправленный список
import java.util.List;

public class parser {

    private List<String> outlink = new LinkedList<>();                                                  //хранение подходящих ссылок, найденных на странице
    private final Logger logger = (Logger) LogManager.getLogger(parser.class);                              //включение логгера
    private static final String user =                                                                      //подпись как браузер, чтобы сайт считал нас не роботом, а браузером
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

    public void parse(String url, HashSet lHash, elasticConnector el) throws IOException {
        Document doc = null;
        for (int i = 1; i <= 6; i++) {                                                      //установление количества повторных попыток подключения, если настал тайм-аут
            try{
                doc = Jsoup.connect(url).timeout(10*1000).userAgent(user).get();                              //подключение к url
                break; // Break immediately if successful
            }
            catch (SocketTimeoutException e){
                // Swallow exception and try again
                logger.warn("jsoup Timeout occurred " + i + " time(s)");
            }
        }

        JSONObject jsonObject = new JSONObject();
        Elements links;
        if (doc != null) {   //если получили документ
            System.out.println("Connected to " + url);  //оповещение
            links = doc.select("a[href]");      //выборка всех ссылок со страницы

            String header = doc.select("div[class=story-body]>h1").text();  //выборка заголовка статьи
            String author = doc.select("div[class=story-body]>div[class=byline]").text()
                    .replace("By ", "");            //выборка автора статьи и убирание "by "
            String pub_date = doc.select("li[class^=mini] > div[class^=date]").text();      //выборка даты написания статьи
            String text = doc.select("div[class^=story-body__inner]").text();               //выборка текста статьи
            text = text.replace(doc.select("div[class^=story-body__inner]> figure").text(),"");     //вырезание из текста мусора от картинки

            XContentBuilder builder = XContentFactory.jsonBuilder();                            //создание конструктора объекта для еластика
            builder.startObject();                                                                  //начало объекта
            {
                builder.field("header", header);                                //добавление заголовка
                builder.field("author", author);                                //добавление автора
                builder.timeField("pub_date", pub_date);                         //добавление даты публикации
                builder.field("text", text);                                      //добавление текста статьи
                builder.field("url", url);                                          //добавление адреса страницы
            }
            builder.endObject();                                                                    //конец объекта

            if ((isInteger(url.substring(url.length()-8)))&&(doc.select("ul[class=tags-list]").text()!="")&&(header.length()>5)){ //если последние 8 символов-цифры И
                System.out.println("Writing to Elasticsearch...");                                                                         // выборка тэгов статьи не пустая И заголовок длиннее 5 символов
                el.getBulkProcessor().add(new IndexRequest("bbc_news")                                                                      //добавить в очередь запрос на внесение в базу
                        .source(builder));
            }

            for (Element l : links) {                   //для каждой найденной ссылки
                String g = l.absUrl("href");    //выбрать абсолютную ссылку (если она вдруг относительная)
                int r = g.hashCode();                       //взять хэш-код ссылки
                HashSet<Integer> f = navigation.getHash();  //загрузить список запрещенных для добавления ссылок
                if (!(f.contains(r))                        //если ссылки нет в запрещенных для добавления
                        &&((g.startsWith("https://www.bbc.com/news"))||(g.startsWith("https://www.bbc.co.uk/news/")))       //если она начинается либо с одного, либо с другого
                        &&!(g.contains("#"))                                                //если не содержит #,?,&
                        &&!(g.contains("?"))
                        &&!(g.contains("&"))) {
                    outlink.add(g);                                                     //добавить в запланированные для посещения
                    //lHash.add(r);                                                       //добавить в запрещенные для добавления
                }
            }

            System.out.println("Found " + outlink.size() + " new links");
        } else {
            System.out.println("Error while connection to URL: " + url);
        }
        System.out.println();
    }

    public List<String> getLinks()          //функция для вывода ссылок для посещения во внешнюю программу
    {
        return this.outlink;
    }

    public static boolean isInteger(String s) {         //проверка на число
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }
}
