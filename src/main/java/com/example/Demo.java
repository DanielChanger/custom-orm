package com.example;

import com.example.model.Product;
import org.h2.jdbcx.JdbcDataSource;


public class Demo {
    public static void main(String[] args) {
        JdbcDataSource jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        SessionFactory sessionFactory = new SessionFactory(jdbcDataSource);
        Session session = sessionFactory.createSession();

        Product product = session.find(Product.class, 1);
        System.out.println(product);
        product.setName("newProductName");

        session.close();
    }
}
