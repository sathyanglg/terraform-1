package com.urbancode.terraform.commands.aws;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.amazonaws.services.ec2.AmazonEC2;
import com.urbancode.terraform.commands.common.Command;
import com.urbancode.terraform.commands.common.CommandException;
import com.urbancode.terraform.tasks.aws.ContextAWS;
import com.urbancode.terraform.tasks.aws.EnvironmentTaskAWS;
import com.urbancode.terraform.tasks.aws.InstanceTask;
import com.urbancode.terraform.tasks.aws.helpers.AWSHelper;
import com.urbancode.terraform.tasks.aws.util.InstancePriorityComparator;

public class ResumeCommand implements Command {

    //**********************************************************************************************
    // CLASS
    //**********************************************************************************************
    static private final Logger log = Logger.getLogger(ResumeCommand.class);

    //**********************************************************************************************
    // INSTANCE
    //**********************************************************************************************
    private ContextAWS context;
    private AWSHelper helper;
    private AmazonEC2 client;

    //----------------------------------------------------------------------------------------------
    public ResumeCommand(ContextAWS context) {
        this.context = context;
        this.helper = new AWSHelper();
        this.client = context.fetchEC2Client();
    }


    //----------------------------------------------------------------------------------------------
    /**
     * This will attempt to start all stopped instances in the environment.
     */
    @Override
    public void execute()
    throws CommandException {
        List<String> instanceIds = getInstanceIds();
        try {
            helper.startInstances(instanceIds, client);
        } catch (RemoteException e) {
            log.warn("RemoteException while attempting to start instance");
            throw new CommandException(e);
        } catch (InterruptedException e) {
            log.warn("InterruptedException while attempting to start instance");
            throw new CommandException(e);
        }
    }

    //----------------------------------------------------------------------------------------------
    private List<String> getInstanceIds() {
        List<String> result = new ArrayList<String>();
        List<InstanceTask> instanceTasks = ((EnvironmentTaskAWS) context.getEnvironment()).getInstances();

        Collections.sort(instanceTasks, new InstancePriorityComparator());
        for (InstanceTask instanceTask : instanceTasks) {
            result.add(instanceTask.getId());
        }
        return result;
    }

}
