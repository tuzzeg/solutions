package com.kaggle.acm.hackathon;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import org.xml.sax.SAXException;
import com.kaggle.acm.hackathon.data.Order;
import com.kaggle.acm.hackathon.data.Product;
import com.kaggle.acm.hackathon.io.ProductReader;
import com.kaggle.acm.hackathon.tfidf.BagOfWords;
import com.kaggle.acm.hackathon.tfidf.Indexed;
import com.kaggle.acm.hackathon.tfidf.Tag;
import com.kaggle.acm.hackathon.tfidf.TfIdfCalculator;
import com.kaggle.acm.hackathon.word.*;
import com.kaggle.acm.hackathon.word.Dictionary;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private final static SimpleDateFormat millis = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    private final static SimpleDateFormat noMillis = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static final int NAME_RANK = 30;
    public static final int MAP_COUNT = 5;

    private Map<String, Indexed<Product>> search;

    private final Bagger bagger = new Bagger(Dictionary.createChecker());

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        if (args.length != 4) {
            System.out.print("Usage: ru.ifmo.ctddev.hackathon.Main <xml product data filename> <train set filename> <test set filename> <output filename>");
            return;
        }

        new Main().start(args[0], args[1], args[2], args[3]);
    }

    private void start(String docFileName, String trainFilename, String testFileName, String outFileName) throws IOException {
        Map<String, Product> productMap = initProducts(docFileName);

        List<Order> orders = readOrders(true, trainFilename);
        OrderHistory history = new OrderHistory(orders);

        indexFromProducts(NAME_RANK, productMap.values());
        indexFromRequests(orders, productMap, history);

        Map<Indexed<Product>, Integer> stats = Maps.newHashMap();
        for (Indexed<Product> doc : search.values()) {
            stats.put(doc, 0);
        }
        for (Order order : orders) {
            Indexed<Product> doc = search.get(order.getSku());
            if (stats.get(doc) != null) {
                stats.put(doc, stats.get(doc) + 1);
            }
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFileName)));
        out.println("sku");

        TfIdfCalculator tfIdfCalculator = new TfIdfCalculator(stats);
        for (Order test : readOrders(false, testFileName)) {
            BagOfWords bag = getBag(test, history);
            List<Product> best = tfIdfCalculator.getBest(bag, test.getRequest().getEntered(), MAP_COUNT);
            out.println(Joiner.on(" ").join(Lists.transform(Lists.newArrayList(best), new Function<Product, String>() {
                public String apply(Product o) {
                    return o.getSku();
                }
            })));
        }
        out.close();
    }

    private void indexFromRequests(List<Order> orders, Map<String, Product> productMap, OrderHistory history) {
        for (Order order : orders) {
            Indexed<Product> x = search.get(order.getSku());
            if (x == null) {
                Product product = productMap.get(order.getSku());
                if (product == null) {
                    product = new Product(order.getSku(), order.getSku(), Product.NO_DATE, Product.NO_RANK);
                    productMap.put(order.getSku(), product);
                }
                x = new Indexed<Product>(product);
                search.put(order.getSku(), x);
            }

            BagOfWords bag = getBag(order, history);
            x.count(bag.getWords());
        }
    }

    private void indexFromProducts(int rank, Collection<Product> products) {
        Collection<Indexed<Product>> index = Lists.newArrayList();
        for (Product p : products) {
            Indexed<Product> e = new Indexed<Product>(p);
            e.count(Lists.transform(bagger.toBag(p.getName()), new Function<String, Tag>() {
                public Tag apply(String s) {
                    return Tag.word(s);
                }
            }), rank);
            e.count(Arrays.asList(Tag.word(p.getSku())));
            index.add(e);
        }

        search = Maps.newHashMap(Maps.uniqueIndex(index, new Function<Indexed<Product>, String>() {
            public String apply(Indexed<Product> productIndexed) {
                return productIndexed.getKey().getSku();
            }
        }));
    }

    private List<Order> readOrders(boolean isTrain, String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName));
        scanner.nextLine();

        List<Order> orders = Lists.newArrayList();
        while (scanner.hasNext()) {
            Scanner internal = new Scanner(scanner.nextLine());
            internal.useDelimiter(",");
            String user = internal.next();
            String sku = isTrain ? internal.next() : null;
            internal.next(); // category
            String query = unquote(internal.next());
            internal.next(); // time hit
            long looked = parseTime(unquote(internal.next()));

            orders.add(new Order(query, sku, looked, user));
        }
        return orders;
    }

    private Map<String, Product> initProducts(String fileName) {
        ProductReader reader = new ProductReader(fileName);
        return reader.readProducts();
    }

    private BagOfWords getBag(Order o, OrderHistory history) {
        BagOfWords bag = new BagOfWords();
        bag.count(Lists.transform(bagger.toBag(o.getRequest().getQuery()), new Function<String, Tag>() {
            public Tag apply(String s) {
                return Tag.word(s);
            }
        }));
        bag.count(Lists.transform(history.getOrdered(o.getUser()), new Function<String, Tag>() {
            public Tag apply(String s) {
                return Tag.also(s);
            }
        }));
        bag.count(Arrays.asList(Tag.date(o.getRequest().getEntered())));
        return bag;
    }

    private static String unquote(String query) {
        return query.replace("\"", "");
    }

    private static long parseTime(String hit) {
        try {
            return millis.parse(hit).getTime();
        } catch (ParseException e) {
            try {
                return noMillis.parse(hit).getTime();
            } catch (ParseException e1) {
                return 0;
            }
        }
    }
}
