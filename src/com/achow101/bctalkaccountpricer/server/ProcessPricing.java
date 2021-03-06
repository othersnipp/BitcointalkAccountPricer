/*
 * Copyright (C) 2016  Andrew Chow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.achow101.bctalkaccountpricer.server;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class ProcessPricing implements Runnable {

    private static BlockingQueue<QueueRequestDB> requestsToProcess = Config.requestsToProcess;
    private static BlockingQueue<QueueRequestDB> processedRequests = Config.processedRequests;

    private volatile boolean stop = false;
	
	public void run()
	{
		System.out.println("Starting ProcessPricing thread for processing the requests");
		
		// Infinte loop so that it runs indefinitely
		while(!stop)
		{
            try {
                // Get request from PricingServiceImpl thread
                QueueRequestDB req = requestsToProcess.take();
                System.out.println("Processing request " + req.getToken());
                req.setProcessing(true);

                // Price the request
                AccountPricer pricer = new AccountPricer(req);
                req.setResult(pricer.getAccountData());
                req.setDone(true);
                req.setCompletedTime(System.currentTimeMillis() / 1000L);
                req.setProcessing(false);
                req.setQueuePos(-5);
                System.out.println("Completed pricing request " + req.getToken());

                //Pass the data back to PricingServiceImpl thread
                processedRequests.put(req);

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
		}
        System.out.println("ProcessPricing thread stopped.");
	}

    public void stopThread()
    {
        stop = true;
    }
}
