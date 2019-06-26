package src;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class navigation {
    private static HashSet<Integer> lHash = new HashSet<Integer>();                     //запрещенные к добавлению в запланированные ссылки, подробнее в парсере
    private LinkedList<String> todoo = new LinkedList<String>();                         //запланированныее ссылки, небезопасно для потоков
    List<String> synlist = Collections.synchronizedList(todoo);                          //как todoo, только безопасно для потоков

    public static HashSet<Integer> getHash() {                                          //метод для передачи множества в другие методы
        return lHash;
    }

    /*synchronized String cURL(Iterator i){
        String n = "";
        if (i.hasNext()) {
            n = (String) i.next();
        }
        System.out.println("next url: " + n);
        return n;
    }*/

    void task (String url, elasticConnector el) {           //задача для потоков - ее можно исполнять нескольким сразу
        parser p = new parser();                            //новый экземпляр парсера для каждого запуска метода
        try {
            p.parse(url, lHash, el);                        //парсим
        } catch (IOException e) {
            e.printStackTrace();                            //если есть ошибка с файлами/потоками - выводим
        }
        System.out.println("Pages visited: " + lHash.size());       //вывод кол-ва посещенных страниц\
        lHash.add(url.hashCode());
        add(p);                                             //вызываем добавление ссылок
    }

    synchronized void add(parser p){
        synlist.addAll(p.getLinks());       //безопасно для многопоточности добавить в список найденные при краулинге ссылки
    }

    synchronized String get(){
        String s = synlist.get(0);              //безопасно для многопоточности взять первую ссылку из запланированных
        synlist.remove(0);                  //удалить первую ссылку
        return s;
    }

    public boolean crawl (String starturl) throws IOException {
        final elasticConnector el = new elasticConnector();         //инициализация коннектора к еластику
        el.init();                                                  //запуск функции с инициализациями клиента и процессора

        ExecutorService service = Executors.newFixedThreadPool(2);  //задание размера пула потоков
        Runnable task = () -> {                                     //создание задачи для множества потоков
            task(get(), el);                                        //выполнять задачу на краулинг (?)
        };

        parser first = new parser();                                //создание экземпляра парсера
        first.parse(starturl, lHash, el);                           //первый прогон. нужен чтобы набрать много ссылок сразу
        synlist.addAll(first.getLinks());                           //добавление всех найденных ссылок в запланированные
        while (synlist.size()>=0){                                  //пока есть запланированные ссылки
            Future result = service.submit(task);                   //запуск многопоточного исполнения задачи
        }
        service.shutdown();                                         //завершение работы потоков
        el.close();                                                 //завершение работы клиента и процессора
        return true;
    }
}
