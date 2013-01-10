package com.kaggle.acm.hackathon.io;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.kaggle.acm.hackathon.data.Product;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;

public class ProductReader {
    private final Document document;

    public ProductReader(String fileName) {
        try {
            File f = new File(fileName);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Product> readProducts() {
        NodeList list = document.getElementsByTagName("product");
        Set<Product> products = Sets.newHashSet();

        for (int i = 0; i < list.getLength(); i++) {
            Node product = list.item(i);
            NodeList properties = product.getChildNodes();
            String sku = null;
            String name = null;
            long startDate = Product.NO_DATE;
            int rank = Product.NO_RANK;

            for (int j = 0; j < properties.getLength(); j++) {
                Node child = properties.item(j);
                if (child.getNodeName().equals("sku")) {
                    sku = child.getTextContent();
                }
                if (child.getNodeName().equals("name")) {
                    name = child.getTextContent().toLowerCase();
                }
                if (child.getNodeName().equals("salesRankMediumTerm")) {
                    if (!child.getTextContent().isEmpty()) {
                        rank = Integer.parseInt(child.getTextContent());
                    }
                }
                if (child.getNodeName().equals("startDate") || child.getNodeName().equals("releaseDate")) {
                    try {
                        startDate = Math.min(startDate == 0 ? System.currentTimeMillis() : startDate, new SimpleDateFormat("yyyy-MM-dd").parse(child.getTextContent()).getTime());
                    } catch (ParseException e) {
                    }
                }
            }

            products.add(new Product(name, sku, startDate, rank));
        }
        return Maps.newHashMap(Maps.uniqueIndex(products, new Function<Product, String>() {
            public String apply(Product product) {
                return product.getSku();
            }
        }));
    }
}
