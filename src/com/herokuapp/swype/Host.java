package com.herokuapp.swype;

import com.herokuapp.swype.Script;
import com.herokuapp.swype.Swype;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Host {

    private Swype swype = new Swype();
    private List<Script> printList = Collections.synchronizedList(new ArrayList<>());

    private Scanner scanner = new Scanner(System.in);

    private Host() {
    	System.out.println("starting app");
    	ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    	
    	//runnable thread to begin printing the script
        Runnable print = () -> {
        	//loop when a script is in queue
        	while (!printList.isEmpty()) {
        		//wait until script has finished printing
                if (!printList.get(0).isPrinted()) {
                	System.out.println("beginning printing of " + printList.get(0).getPrint_id());
                	//set script printing state to 'printing'
                    printList.get(0).setPrinted(true);
                    try {
                    	//begin printing class
                        new Robot(printList.get(0).getScript().split(System.lineSeparator()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //script finished printing. waiting for user to begin next print
                    System.out.println("Printed. Press ENTER for next");
                    scanner.next();
                    Thread t = new Thread(new done(printList.get(0)));
                    t.start();
                }
            }
        };
    	
        Runnable fetch = () -> {
        	System.out.println("fetching");
            Call<Script[]> call = swype.service.getQueue();
            call.enqueue(new Callback<Script[]>() {
                @Override
                public void onResponse(Call<Script[]> call, Response<Script[]> response) {
                    for (int i = 0; i < response.body().length; i++) {
                        if (!inQueue(response.body()[i].getPrint_id())) {
                        	System.out.println("added " + response.body()[i].getPrint_id() + " to array");
                            printList.add(response.body()[i]);
                        }
                    }
                    print.run();
                }

                @Override
                public void onFailure(Call<Script[]> call, Throwable t) {
                	System.out.println(t.getMessage());
                }
            });
        };
        //fetch scripts every 2 minutes
        scheduledExecutorService.scheduleAtFixedRate(fetch, 0, 2, TimeUnit.MINUTES);
    }

    public static void main(String[] args) {
        new Host();
    }

    private boolean inQueue(int id) {
    	//check if script is in queue
        for (Script s : printList) {
            if (id == s.getPrint_id()) {
                return true;
            }
        }
        return false;
    }

    class done implements Runnable {
        Script id;

        done(Script id) {
            this.id = id;
        }
        //delete script from queue database
        @Override
        public void run() {
            Call<Void> call = swype.service.removeTask(id.getId());
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    System.out.println("removed " + printList.get(0).getPrint_count());
                    printList.remove(0);
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {

                }
            });
        }
    }

}
