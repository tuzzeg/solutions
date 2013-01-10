/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author David Thomas
 */
public class SkuMetaDataParser {
    public static class MetaData
    {
        private final Element el;
        public MetaData(Element el) { this.el = el; }
        private String name = null;
        private Date date = null;
        private List<String> purchasedWith = null;
        public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        public String getName()
        {
            if(name==null)
            {
                name = getTextValue(el,"name");
            }
            return name;
        }
        public Date getStartDate() throws Exception
        {
            if(date==null)
            {
                String sdate = getTextValue(el,"startDate");
                date = format.parse(sdate);
            }
            
            return date;
        }
        
        public List<String> getPurchasedWith()
        {
            if(purchasedWith==null)
            {
               NodeList nl = el.getElementsByTagName("frequentlyPurchasedWith");
               purchasedWith = new ArrayList<String>();
               if(nl.getLength()==1)
               {
                   Element ele = (Element)nl.item(0);
                   nl = ele.getElementsByTagName("sku");
                   for(int i=0;i<nl.getLength();i++)
                   {
                      
                       purchasedWith.add(nl.item(i).getFirstChild().getNodeValue());
                   }
               }
            }
            
            return purchasedWith;
        }
        
    }
    
   
    
    private final Map<String,MetaData> skuMap = new HashMap<String,MetaData>();
   
    public SkuMetaDataParser(File file) throws Exception
    {
        create(file);
    }
    
    public Map<String,MetaData> getSkuMap() { return skuMap; }
   
    private void create(File file) throws Exception
    {
        Map<String, MetaData> map = skuMap;
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document dom = db.parse(file);
        
        Element root = dom.getDocumentElement();
        NodeList nl = root.getElementsByTagName("product");
        for(int i=0;i<nl.getLength();i++)
        {
            Element el = (Element)nl.item(i);
            String sku = getTextValue(el,"sku");
            String name = getTextValue(el,"name");
            MetaData md = new MetaData(el);
        //    System.out.print(name + " purchasedWith ");
        //    for(String s:md.getPurchasedWith()) System.out.print(s + ",");
        //    System.out.println();
            map.put(sku, md);
        }    
    }
    
  
    
    	/**
	 * I take a xml element and the tag name, look for the tag and get
	 * the text content
	 * i.e for <employee><name>John</name></employee> xml snippet if
	 * the Element points to employee node and tagName is 'name' I will return John
	 */
	private static String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}
    
}
