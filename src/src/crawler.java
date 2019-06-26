package src;

import java.io.IOException;

public class crawler {
    public static void main (String[] str) throws IOException {
        navigation pr = new navigation();                               //создание экземпляра класса навигация
        pr.crawl("https://www.bbc.com/news");                    //начало поиска. Дальше идти в файл navigation
    }
}
