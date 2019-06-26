package src;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.io.IOException;

public class elasticConnector {
    private final Logger logger = (Logger) LogManager.getLogger(elasticConnector.class);        //логгер
    RestHighLevelClient client;                                                                 //объявление высокоуровневого клиента
    BulkProcessor bulkProcessor;                                                                //объявление процессора очереди (?)

    public void init () throws IOException {
        {
            client = new RestHighLevelClient(                                                   //инициализация клиента с 6 портами
                    RestClient.builder(
                            new HttpHost("localhost", 9200, "http"),
                            new HttpHost("localhost", 9201, "http"),
                            new HttpHost("localhost", 9202, "http"),
                            new HttpHost("localhost", 9203, "http"),
                            new HttpHost("localhost", 9204, "http"),
                            new HttpHost("localhost", 9205, "http")));
        }
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {        //инициализация объекта, который слушает ответ базы на запросы от процессора
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                int numberOfActions = request.numberOfActions();
                logger.debug("Executing bulk [{}] with {} requests",
                        executionId, numberOfActions);
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request,
                                  BulkResponse response) {
                if (response.hasFailures()) {
                    logger.warn("Bulk [{}] executed with failures", executionId);
                } else {
                    logger.debug("Bulk [{}] completed in {} milliseconds",
                            executionId, response.getTook().getMillis());
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request,
                                  Throwable failure) {
                logger.error("Failed to execute bulk", failure);
            }
        };

        BulkProcessor.Builder builder = BulkProcessor.builder(                                  //инициализация конструктора процессора
                (request, bulkListener) ->
                    client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),            //исполнение асинхронное
                listener);
        builder.setBulkActions(500);                                                         //размер очереди
        builder.setBulkSize(new ByteSizeValue(1L, ByteSizeUnit.MB));                     //физический размер очереди
        builder.setConcurrentRequests(0);
        builder.setFlushInterval(TimeValue.timeValueSeconds(10L));                          //раз в сколько секунд запроос отправится при несрабатывании других условий
        builder.setBackoffPolicy(BackoffPolicy
                .constantBackoff(TimeValue.timeValueSeconds(1L), 3));       //сколько раз и с какой частотой будут отправляться неудачные запросы
        bulkProcessor = builder.build();                                                         //инициализация процессора
    }
    public void close (){                                                               //метод для завершения работы клиента и процессора
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bulkProcessor.close();
    }
    public RestHighLevelClient getclient () throws IOException {                        //метод вывода клиента во внешнюю программу
        return client;
    }
    public BulkProcessor getBulkProcessor () {
        return bulkProcessor;                                                   //метод вывода процессора во внешнюю программу
    }
}
