package io.jenkins.plugins.talend;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Item;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

import com.talend.tmc.dom.Advanced;
import com.talend.tmc.dom.ExecutionPromotionRequest;
import com.talend.tmc.dom.ExecutionPromotionResponse;
import com.talend.tmc.dom.ExecutionResponse;

import com.talend.tmc.dom.enums.ArtifactType;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.executions.ExecutionPromotionService;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.kohsuke.stapler.AncestorInPath;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class RunPromotionBuilder extends Builder implements SimpleBuildStep {
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

    private String tPromotion = "";
    private String tArtifactType = "";
    private String tEnvironment = "";
    private String tWorkspace = "";
    private String tTask = "";
    private String tPlan = "";
    private String tArtifact = "";
    private boolean tKeepTargetResources = false;
    private boolean tKeepTargetRunProfiles = false;
    
    @DataBoundConstructor
    public RunPromotionBuilder() {
    }	

    public String getPromotion() {
        return tPromotion;
    }

    @DataBoundSetter
    public void setPromotion(String value) {
        this.tPromotion = value;
        this.tEnvironment = tPromotion.split(" ")[0];
    }

    public String getEnvironment() {
        return tEnvironment;
    }

    public String getArtifactType() {
        return tArtifactType;
    }

    @DataBoundSetter
    public void setArtifactType(String value) {
        this.tArtifactType = value;
    }

    public String getWorkspace() {
        return tWorkspace;
    }

    @DataBoundSetter
    public void setWorkspace(String value) {
        this.tWorkspace = value;
    }

    public String getTask() {
        return tTask;
    }

    @DataBoundSetter
    public void setTask(String value) {
        this.tTask = value;
    }
    
    public String getPlan() {
        return tPlan;
    }

    @DataBoundSetter
    public void setPlan(String value) {
        this.tPlan = value;
    }

    public String getArtifact() {
        return tArtifact;
    }

    @DataBoundSetter
    public void setArtifact(String value) {
        this.tArtifact = value;
    }

    public boolean getKeepTargetResources() {
        return tKeepTargetResources;
    }

    @DataBoundSetter
    public void setKeepTargetResources(boolean value) {
        this.tKeepTargetResources = value;
    }

    public boolean getKeepTargetRunProfiles() {
        return tKeepTargetRunProfiles;
    }

    @DataBoundSetter
    public void setKeepTargetRunProfiles(boolean value) {
        this.tKeepTargetRunProfiles = value;
    }

    /**
     * 
     * Outdated for api version 1.2, but good to glance over
     * @see https://community.talend.com/s/article/Using-the-Talend-Cloud-Management-Console-Public-API-O2Ndn?language=en_US
     * @see https://help.talend.com/r/en-US/Cloud/management-console-with-pipeline-designer/manage-promotion 
     */
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("*** RUNPROMOTION ***");
		listener.getLogger().println("promotion = " + tPromotion);
		listener.getLogger().println("artifactType = " + tArtifactType);
		listener.getLogger().println("environment = " + tEnvironment);
		listener.getLogger().println("workspace = " + tWorkspace);
		listener.getLogger().println("task = " + tTask);
		listener.getLogger().println("plan = " + tPlan);
		listener.getLogger().println("artifact = " + tArtifact);

		TalendCredentials credentials = TalendLookupHelper.getTalendCredentials();
		TalendCloudRegion region = TalendLookupHelper.getTalendRegion();

		try {
        	String artifactId = "";
        	String promotionId = TalendLookupHelper.getPromotionIdByName(tPromotion);
        	switch (tArtifactType) {
        		case "ENVIRONMENT": 
        			artifactId = TalendLookupHelper.getEnvironmentIdByName(tEnvironment);
        			break;
        		case "WORKSPACE":
        			artifactId = TalendLookupHelper.getWorkspaceIdByName(tEnvironment, tWorkspace);
        			break;
        		case "PLAN":
        			artifactId = TalendLookupHelper.getPlanIdByName(tEnvironment, tWorkspace, tPlan);        			
        			break;
        		case "FLOW":
        			artifactId = TalendLookupHelper.getTaskIdByName(tEnvironment, tWorkspace, tTask);
        			break;
        		case "ACTION":
        			artifactId = TalendLookupHelper.getArtifactIdByName(tEnvironment, tWorkspace, tArtifact);
        			break;
        		default: 
        			artifactId = "";
        	}
            LOGGER.info("artifactid = " + artifactId);
        	
        	if (!artifactId.isEmpty() || tArtifactType.equals("ENVIRONMENT")) {
				ExecutionPromotionRequest promotionRequest = new ExecutionPromotionRequest();
				promotionRequest.setExecutable(promotionId);
				promotionRequest.setKeepTargetResources(tKeepTargetResources);
				promotionRequest.setKeepTargetRunProfiles(tKeepTargetRunProfiles);
				if (!tArtifactType.equals("ENVIRONMENT")) {
					Advanced advanced = new Advanced();
					advanced.setArtifactId(artifactId);
					advanced.setArtifactType(tArtifactType);
					promotionRequest.setAdvanced(advanced);
				}
	            LOGGER.info("Going to promote " + promotionRequest.toString() );


	            ExecutionPromotionService executionPromotionService = ExecutionPromotionService.instance(credentials, region);
	            ExecutionResponse executionResponse = executionPromotionService.post(promotionRequest);
	            LOGGER.info("Promotion started. The ID of the execution is" + executionResponse.getExecutionId() );

                ExecutionPromotionResponse execution = executionPromotionService.get(executionResponse.getExecutionId());
                if (!execution.getStatus().equals("PROMOTED")) {
                    throw new InterruptedException("Job Completed in non Successful State :" + execution.toString());
                } else {
                	//TODO: Parse full Promotion Report
                	LOGGER.info("Job Finished Succesfully");
                }
        	} else {
        		throw new InterruptedException("There is nothing to promote");
        	}
    		listener.getLogger().println("*** RUNPROMOTION ***");            
            Thread.sleep(10);  // to include the InterruptedException
        } catch(TalendRestException ex){
        	listener.getLogger().println(ex.getMessage());
        	run.setResult(Result.FAILURE);
        	throw new InterruptedException (ex.getMessage());
        }
          catch(Exception e) {
        	listener.getLogger().println(e.getMessage());
        	run.setResult(Result.FAILURE);
          }
    }

    @Symbol("runPromotion")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	private int jsLastPromotion;
    	
    	@POST
        public ListBoxModel doFillPromotionItems(@CheckForNull @AncestorInPath Item item, @QueryParameter String artifactType) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	return TalendLookupHelper.getPromotionList();
        }
    	
    	@POST
        public ListBoxModel doFillArtifactTypeItems(@AncestorInPath Item item, @QueryParameter String promotion) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
            model.add("", "");
            model.add("Environment", "ENVIRONMENT");
            model.add("Workspace", ArtifactType.WORKSPACE.toString());
            model.add("Plan", ArtifactType.PLAN.toString());
            model.add("Task", ArtifactType.FLOW.toString());
            model.add("Artifact",ArtifactType.ACTION.toString());
            return model;
        }

    	@POST
        public ListBoxModel doFillWorkspaceItems(@AncestorInPath Item item, @QueryParameter String promotion, @QueryParameter String artifactType) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	if (!promotion.isEmpty()) {
	        	String environment = promotion.split(" ")[0];
				return TalendLookupHelper.getWorkspaceList(environment);
        	}
        	return model;
		}

    	@POST
    	public ListBoxModel doFillTaskItems(@AncestorInPath Item item, @QueryParameter String promotion, @QueryParameter String workspace) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	if (!promotion.isEmpty() && !workspace.isEmpty()) {
	        	String environment = promotion.split(" ")[0];
				return TalendLookupHelper.getTaskList(environment,workspace);
        	}
        	return model;
		}

    	@POST
    	public ListBoxModel doFillPlanItems(@AncestorInPath Item item, @QueryParameter String promotion, @QueryParameter String workspace) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	if (!promotion.isEmpty() && !workspace.isEmpty()) {
	        	String environment = promotion.split(" ")[0];
				return TalendLookupHelper.getPlanList(environment,workspace);
        	}
        	return model;
		}

    	@POST
    	public ListBoxModel doFillArtifactItems(@AncestorInPath Item item, @QueryParameter String promotion, @QueryParameter String workspace) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	if (!promotion.isEmpty() && !workspace.isEmpty()) {
	        	String environment = promotion.split(" ")[0];
				return TalendLookupHelper.getArtifactList(environment,workspace);
        	}
        	return model;
		}
    	
    	@POST
    	public FormValidation doCheckArtifactType(@AncestorInPath Item item, @QueryParameter String artifactType)
                throws IOException, ServletException {
            if (item == null) { // no context
                return FormValidation.error("No context");
            }
            item.checkPermission(Item.CONFIGURE);
            if (artifactType.length() < 2)
                return FormValidation.warning("please select an Artifact Type");
            return FormValidation.ok();
        }
     
        @JavaScriptMethod
        public synchronized String createPromotionId() {
            return String.valueOf(jsLastPromotion++);
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.PromotionBuilder_DescriptorImpl_DisplayName();
        }

    }

}
