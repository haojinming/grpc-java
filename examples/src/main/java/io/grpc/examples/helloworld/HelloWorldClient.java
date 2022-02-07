/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

class GrpcGreetingRepeat implements Runnable{
  private final String allStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private Logger logger;
  private Random random = new Random();

  private String RandomStr() {
    StringBuffer sb = new StringBuffer();
    int length = random.nextInt(512);
    length = length % 512;
    for(int i = 0; i < length; i++){
      int number = random.nextInt(62);
      sb.append(allStr.charAt(number));
    }
    return sb.toString();
  }
  public GrpcGreetingRepeat(ManagedChannel channel, int index, int greetNum) {
    
    this.channel = channel;
    this.greetNum = greetNum;
    this.index = index;
    String logPath = "./grpc_duration_" + index + ".log";
    try {
      logger = Logger.getLogger(GrpcGreetingRepeat.class.getName() + index);
      FileHandler fileHander = new FileHandler(logPath);
      logger.setUseParentHandlers(false);
      logger.addHandler(fileHander);
    } catch (Exception e) {
      
    }
    
  }
    /** Say hello to server. */
  public void greet(String name, int num) {
    // logger.info("Will try to greet " + name + " ...");
    HelloReply response;
    GreeterGrpc.GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(channel);
    try {
      for (int i = 0; i < num; i++) {
        String content = name + RandomStr();
        HelloRequest request = HelloRequest.newBuilder().setName(content).build();
        long startTime = System.currentTimeMillis();
        response = blockingStub.sayHello(request);
        long curTime = System.currentTimeMillis();
        long duration = curTime - startTime;
        if (duration > 0) {
          String msg = "Greeting: " + response.getMessage() + " takes: " +  (curTime - startTime) + " ms";
          //logger.info(msg);
        }
        
      }
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    } finally {
      
    }
  }

  @Override
  public void run() {
    greet("Concur " + index, greetNum);
  }
  ManagedChannel channel;
  private int greetNum;
  private int index;
}

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
  /** Construct client for accessing HelloWorld server using the existing channel. */
  public HelloWorldClient(Channel channel) {
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting. The second argument is the target server.
   */
  public static void main(String[] args) throws Exception {
    // Access a service running on the local machine on port 50051
    String target = "localhost:50051";
    int concurNum = 10;
    int greetNum = 1;
    // Allow passing in the user and target strings as command line arguments
    if (args.length > 0) {
      if ("--help".equals(args[0])) {
        System.err.println("Usage: [name [target]]");
        System.err.println("");
        System.err.println("  name    The name you wish to be greeted by. Defaults to ");
        System.err.println("  target  The server to connect to. Defaults to " + target);
        System.exit(1);
      }
    }
    if (args.length >= 2) {
      concurNum = Integer.valueOf(args[0]);
      greetNum = Integer.valueOf(args[1]);
    }

    if (concurNum <=0 || greetNum <= 0) {
      System.out.println("input num should be larger than 0, input: " + args[0] + " " + args[1]);
      return;
    }
    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext()
        .build();
    ArrayList<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < concurNum; i++) {
      GrpcGreetingRepeat repeatGreet = new GrpcGreetingRepeat(channel, i, greetNum);
      Thread t = new Thread(repeatGreet);
      t.start();
      threads.add(t);
    }

    for (Thread t : threads) {
      t.join();
    }
    
    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);

  }
}
