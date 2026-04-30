package com.checkorix;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.checkorix.handler.ExecuteHandler;
import com.checkorix.handler.StaticHandler;
import com.sun.net.httpserver.HttpServer;

public class CheckorixServer {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.createContext("/checkorix-release", new StaticHandler());
        server.createContext("/execute", new ExecuteHandler());

        server.start();

        System.out.println("Checkorix running on http://localhost:8081");
    }
}