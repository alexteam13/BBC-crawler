import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class navigation {
    private HashSet<Integer> lHash = new HashSet<Integer>();
    private LinkedList<String> todo = new LinkedList<String>();

    /*synchronized String cURL(Iterator i){
        String n = "";
        if (i.hasNext()) {
            n = (String) i.next();
        }
        System.out.println("next url: " + n);
        return n;
    }*/

    void task (String url, elasticConnector el) {
        parser p = new parser();
        try {
            p.parse(url, lHash, el);
        } catch (IOException e) {
            e.printStackTrace();
        }
        add(p);
    }

    synchronized void add(parser p){
        todo.addAll(p.getLinks());
    }

    synchronized String get(){
        return todo.pollFirst();
    }

    public boolean crawl (String starturl) throws IOException {
        final elasticConnector el = new elasticConnector();
        el.init();
        parser first = new parser();
        first.parse(starturl,lHash, el);
        lHash.add(starturl.hashCode());
        todo.addAll(first.getLinks());
        System.out.println(todo.size());
        while (todo.size()>=0){
            Runnable task = () -> {
                task(get(), el);
             };
            ExecutorService service = Executors.newFixedThreadPool(3);
            Future result = service.submit(task);
            service.shutdown();
        }
        el.close();
        return true;
    }
    public HashSet<Integer> getlHash(){
        return this.lHash;
    }
}
