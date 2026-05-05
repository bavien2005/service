package org.anta;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class ProductServiceApplication {

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}