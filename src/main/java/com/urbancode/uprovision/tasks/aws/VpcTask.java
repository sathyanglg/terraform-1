package com.urbancode.uprovision.tasks.aws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.urbancode.uprovision.tasks.EnvironmentCreationException;
import com.urbancode.uprovision.tasks.EnvironmentDestructionException;
import com.urbancode.uprovision.tasks.aws.helpers.AWSHelper;
import com.urbancode.uprovision.tasks.common.Task;


public class VpcTask extends Task {
    
    //**********************************************************************************************
    // CLASS
    //**********************************************************************************************
    final static private Logger log = Logger.getLogger(VpcTask.class);
    
    //**********************************************************************************************
    // INSTANCE
    //**********************************************************************************************
    
    private AmazonEC2 ec2Client;
    private AWSHelper helper;
    private ContextAWS context;
    
    private String vpcId;
    private String cidr;
    private InetGwyTask inetGwy;
    
    private List<SubnetTask> subnets = new ArrayList<SubnetTask>();
    private List<VpcSecurityGroupTask> securityGroups = new ArrayList<VpcSecurityGroupTask>();
    private List<RouteTableTask> routeTables = new ArrayList<RouteTableTask>();

    //----------------------------------------------------------------------------------------------
    public VpcTask(ContextAWS context) {
        this.context = context;
        helper = context.getAWSHelper();
    }
    
    //----------------------------------------------------------------------------------------------
    public void setId(String id) {
        this.vpcId = id;
    }

    //----------------------------------------------------------------------------------------------
    public void setCidr(String cidr) {
        this.cidr = cidr;
    }
    
    //----------------------------------------------------------------------------------------------
    public String getId() {
        return vpcId;
    }
    
    //----------------------------------------------------------------------------------------------
    public String getCidr() {
        return cidr;
    }
    
    //----------------------------------------------------------------------------------------------
    public List<SubnetTask> getSubnet() {
        return Collections.unmodifiableList(subnets);
    }
    
    //----------------------------------------------------------------------------------------------
    public List<VpcSecurityGroupTask> getSecurityGroupsList() {
        return Collections.unmodifiableList(securityGroups);
    }
    
    //----------------------------------------------------------------------------------------------
    public InetGwyTask getInetGwy() {
        return inetGwy;
    }
    
    //----------------------------------------------------------------------------------------------
    public List<RouteTableTask> getRouteTables() {
        return Collections.unmodifiableList(routeTables);
    }
    
    //----------------------------------------------------------------------------------------------
    public SubnetTask findSubnetForName(String name) {
        SubnetTask result = null;
        for (SubnetTask subnet : subnets) {
            if (subnet.getName().equals(name)) {
                result = subnet;
                break;
            }
        }
        return result;
    }
    
    //----------------------------------------------------------------------------------------------
    public VpcSecurityGroupTask findSecurityGroupForName(String name) {
        VpcSecurityGroupTask result = null;
        for (VpcSecurityGroupTask group : securityGroups) {
            if (group.getName().equals(name)) {
                result = group;
                break;
            }
        }
        return result;
    }
    
    //----------------------------------------------------------------------------------------------
    public SubnetTask createSubnet() {
        SubnetTask subnet = new SubnetTask(context);
        subnets.add(subnet);
        
        return subnet;
    }
    
    //----------------------------------------------------------------------------------------------
    public VpcSecurityGroupTask createVpcSecurityGroup() {
        VpcSecurityGroupTask group = new VpcSecurityGroupTask(context);
        securityGroups.add(group);
        
        return group;
    }

    //----------------------------------------------------------------------------------------------
    public InetGwyTask createInetGwy() {
        inetGwy = new InetGwyTask(context);
        
        return inetGwy;
    }
    
    //----------------------------------------------------------------------------------------------
    public RouteTableTask createRouteTable() {
        RouteTableTask routeTable = new RouteTableTask(context);
        routeTables.add(routeTable);
        
        return routeTable;
    }

    //----------------------------------------------------------------------------------------------
    private void startVpc() {
        if (vpcId == null) {
            log.info("Starting Vpc...");
            setId(helper.createVpc(cidr, ec2Client));
            log.info("Vpc created with id: " + vpcId);
        }
        else {
            log.info("Vpc with id " + vpcId + " already exists!");
        }
    }
    
    //----------------------------------------------------------------------------------------------
    private void startInetGwy() {
        if (inetGwy != null && inetGwy.getId() == null) {
            log.info("Starting Internet Gateway...");
            inetGwy.setVpcId(vpcId);
            inetGwy.create();
        }
        else {
            if (inetGwy == null) {
                log.info("No Internet Gateway specified.");
            }
            else {
                log.info("Internet Gateway with id "+ inetGwy.getId() + " already exists.");
            }
        }
    }
    
    //----------------------------------------------------------------------------------------------
    private void startSubnets() throws Exception {
        if (subnets != null || subnets.size() != 0) {
            for (SubnetTask subnet : subnets) {
                subnet.setVpcId(vpcId);
                subnet.create();
            }
        }
        else {
            log.info("No subnets specified.");
        }
    }
    
    //----------------------------------------------------------------------------------------------
    private void startRouteTables() 
    throws Exception {
        boolean first = true;
        if (getRouteTables() != null && !getRouteTables().isEmpty()) {
            for (RouteTableTask table : getRouteTables()) {
                if (table.getId() == null) {
                    if ( first ) { 
                        table.setRouteTarget(inetGwy.getId());
                        table.setMainTable(true);
                        first = false;
                    }
                    log.info("Starting Route Table...");
                    table.setVpcId(vpcId);
                    table.create();
                }
                else {
                    log.info("Route Table with id " + table.getId() + " already exists.");
                }
            }
        }
        log.info("No Route Tables specified.");
    }
    
    //----------------------------------------------------------------------------------------------
    private void startSecurityGroups() {
        if (securityGroups != null && !securityGroups.isEmpty()) {
            for (VpcSecurityGroupTask group : securityGroups) {
                    if (group.getId() == null) {
                    log.info("Starting Security Groups...");
                    group.setVpcId(vpcId);
                    group.create();
                }
                else {
                    log.info("Security Group with id " + group.getId() + " already exists.");
                }
            }
        }
        else { 
            log.info("No Security Groups specified.");
        }
    }
    
    //----------------------------------------------------------------------------------------------
    public boolean existsInAws() {
        // since we have an id, check if Vpc exists in AWS
        boolean result = false;
        DescribeVpcsRequest vpcReq = new DescribeVpcsRequest().withVpcIds(vpcId);
        if (context.getEC2Client().describeVpcs(vpcReq) == null) {
            log.info("Vpc with id " + vpcId + " does NOT exist in AWS!"); 
        }
        else {
            result = true;
        }
        return result;
    }
    
    //----------------------------------------------------------------------------------------------
    @Override
    public void create() 
    throws EnvironmentCreationException {
        if (ec2Client == null) {
            ec2Client = context.getEC2Client();
        }
        
        log.info("Creating Vpc...");
        
        try {
            startVpc();
            startInetGwy();
            startSubnets();
            startRouteTables();
            startSecurityGroups();
        }
        catch (Exception e) {
            log.error("EXCEPTION CAUGHT WHEN CREATING VPC", e);
            throw new EnvironmentCreationException("Could not completely create Vpc", e);
        }
        finally {
            ec2Client = null;
        }
    }
    
    //----------------------------------------------------------------------------------------------
    @Override
    public void destroy() 
    throws EnvironmentDestructionException {
        if (ec2Client == null) {
            ec2Client = context.getEC2Client();
        }
        
        log.info("Destroying Vpc...");
        
        try {
            // detach all gateways
    //        helper.detachGateway(getInetGwy().getId(), vpcId, ec2Client);
            // disassociate all route tables
    
            helper.waitForPublicAddresses(ec2Client);
            
            getInetGwy().setVpcId(vpcId);
            getInetGwy().destroy();
            
            helper.waitForPublicAddresses(ec2Client);
            
            // detach all ENIs - there are none?
    
            // remove route tables
            if (getRouteTables() != null && !getRouteTables().isEmpty()) {
                for (RouteTableTask table : getRouteTables()) {
                    table.destroy();
                }
            }
            
            // remove security Groups
            if (securityGroups != null && securityGroups.size() != 0) {
                for (VpcSecurityGroupTask group : securityGroups) {
                    group.destroy();
                }
            }
            
            // remove Subnets
            if (subnets != null && subnets.size() != 0) {
                for (SubnetTask subnet : subnets) {
                    subnet.destroy();
                }
            }
            
            // delete Vpc
            helper.deleteVpc(vpcId, ec2Client);
            
            vpcId = null;
        }
        catch (Exception e) {
            log.error("EXCEPTION CAUGHT WHEN DESTROYING VPC", e);
            throw new EnvironmentDestructionException("Could not completely destroy Vpc", e);
        }
        finally {
            ec2Client = null;
        }
    }
}