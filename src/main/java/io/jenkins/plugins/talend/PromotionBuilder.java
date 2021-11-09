package io.jenkins.plugins.talend;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.talend.Messages;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Item;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
//import hudson.RelativePath;


import com.talend.tmc.dom.Executable;
import com.talend.tmc.dom.ExecutionRequest;
import com.talend.tmc.dom.ExecutionResponse;
import com.talend.tmc.dom.Promotion;

import com.talend.tmc.dom.enums.ArtifactType;
import com.talend.tmc.services.*;
import com.talend.tmc.services.executables.ExecutablePromotionService;
import com.talend.tmc.services.executables.ExecutableService;
import com.talend.tmc.services.executions.ExecutionService;

//import antlr.collections.List;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.kohsuke.stapler.AncestorInPath;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
//import java.util.Arrays;
//import java.util.Hashtable;
import java.util.stream.Collectors;

import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class PromotionBuilder extends Builder implements SimpleBuildStep {
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

    private String tPromotion;
    private boolean tkeepTargetResources;
    private boolean tkeepTargetRunProfiles;
    private ArtifactType tArtifactType;
    private String tArtifactId;
    
    @DataBoundConstructor
    public PromotionBuilder() {
    }	

    public String getPromotion() {
        return tPromotion;
    }

    @DataBoundSetter
    public void setPromotion(String value) {
        this.tPromotion = value;
    }

    public boolean tkeepTargetResources() {
        return tkeepTargetResources;
    }

    @DataBoundSetter
    public void setkeepTargetResources(boolean value) {
        this.tkeepTargetResources = value;
    }

    public boolean keepTargetRunProfiles() {
        return tkeepTargetRunProfiles;
    }

    @DataBoundSetter
    public void setkeepTargetRunProfiles(boolean value) {
        this.tkeepTargetRunProfiles = value;
    }

    public ArtifactType getArtifactType() {
        return tArtifactType;
    }

    @DataBoundSetter
    public void setArtifactType(ArtifactType value) {
        this.tArtifactType = value;
    }

    public String getArtifactId() {
        return tArtifactId;
    }

    @DataBoundSetter
    public void setArtifactId(String value) {
        this.tArtifactId = value;
    }
    

    /**
     * 
     * Outdated for api version 1.2, but good to glance over
     * @see https://community.talend.com/s/article/Using-the-Talend-Cloud-Management-Console-Public-API-O2Ndn?language=en_US
     * @see https://help.talend.com/r/en-US/Cloud/management-console-with-pipeline-designer/manage-promotion 
     */
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String token = TalendConfiguration.get().getCredentialsid();
        String region = TalendConfiguration.get().getRegion();
        String id = "";
        
        StringCredentials stringCredentials = CredentialsHelper.lookupSystemCredentials(token);

		String api = stringCredentials.getSecret().getPlainText();
        TalendCredentials credentials = new TalendBearerAuth(api);
        listener.getLogger().println("region="+region);

        try {
            ExecutionService executionService = ExecutionService.instance(credentials, TalendCloudRegion.valueOf(region));

            //ExecutableService executableService = ExecutableService.instance(credentials, TalendCloudRegion.valueOf(region));

//            String envName = tWorkspace.length() > 0 ? tWorkspace : "default";

           // StringBuilder query = new StringBuilder();
         //   query.append("name="+tJob+"&WorkspaceId="+envName);
            
            		
            /*
            Executable[] executables = executableService.getById(tJob);


            if (executables.length > 1) {
            	listener.getLogger().println("More than 1 Job returned with that name!");
            	return;
            }
            if (executables.length == 0) {
            	listener.getLogger().println("No Job or Task found with that name!");
            	return;
            }
            id = executables[0].getExecutable();
*/
     /*       ExecutableTask executableTask = ExecutableTask.instance(credentials, TalendCloudRegion.valueOf(region));
        	listener.getLogger().println("No Job or Task found with that name!");
            Task task = executableTask.getById(tJob);
        	listener.getLogger().println("No Job or Task found with that name!");
            id = task.getId();
            */
            listener.getLogger().println("Found Job: " + id);
            
            
            
            
            

            ExecutionRequest executionRequest = new ExecutionRequest();
            executionRequest.setExecutable(id);

//            ExecutionPromotionRequest promotionRequest = new ExecutionPromotionRequest();
//            promotionRequest.setExecutable(id);
//            promotionRequest.setKeepTargetResources(true);
            //promotionRequest.getAdvanced().setArtifactId(artifactId);
            
            
            
            /* Execution We can do this later

            Hashtable<String, String> parameters = null;
            if (Cli.hasCliValue("cv")) {
                String[] pairs = Cli.getCliValue("cv").split(";");
                parameters = new Hashtable<>();
                for (String pair : pairs) {
                    String[] nv = pair.split("=");
                    parameters.put(nv[0], nv[1]);
                }

                executionRequest.setParameters(parameters);
            }

 */
            ExecutionResponse executionResponse = executionService.post(executionRequest);
            listener.getLogger().println("Talend Job Started: " + executionResponse.getExecutionId());
            
            Thread.sleep(10);  // to include the InterruptedException
        } catch(TalendRestException | IOException | InterruptedException ex){
        	listener.getLogger().println(ex.getMessage());
        	
        }
          catch(Exception e) {
        	  listener.getLogger().println(e.getMessage());
          }

    }

    @Symbol("Promotion")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public ListBoxModel doFillPromotionItems(@CheckForNull @AncestorInPath Item context) {
            String token = TalendConfiguration.get().getCredentialsid();
            String region = TalendConfiguration.get().getRegion();
            ListBoxModel model = new ListBoxModel();

            TalendCredentials credentials = null;
            ExecutablePromotionService promotionService = null;
        	
        	
        	credentials = new TalendBearerAuth(token);

    		promotionService = ExecutablePromotionService.instance(credentials, TalendCloudRegion.valueOf(region));
    		try {
    			Promotion[] promotions = promotionService.getByQuery(null);
    			java.util.List<String> idList = new ArrayList<String>();  
    			for (Promotion promotion : promotions) {
            		String id = promotion.getExecutable();
            		java.util.List<String> idFilter = idList
                            .stream()
                            .filter(x -> x.equals(id))
                            .collect(Collectors.toList());
            		if (idFilter.size() == 0) {
            			String name = promotion.getName() + " (" + promotion.getSourceEnvironment().getName() + " > " + promotion.getTargetEnvironment().getName() + " )";
            			model.add(name, promotion.getExecutable());
                		idList.add(id);
            		}
            	}          
            } 
    		catch(TalendRestException | IOException ex){
            	System.out.println(ex.getMessage());
            	return null;
            }

            return model;
        }

        public ListBoxModel doFillArtifactTypeItems(@QueryParameter String promotion) {
            ListBoxModel model = new ListBoxModel();
            model.add("Workspace", ArtifactType.WORKSPACE.toString());
            model.add("Plan", ArtifactType.PLAN.toString());
            model.add("Task", ArtifactType.FLOW.toString());
            model.add("Artifact",ArtifactType.ACTION.toString());
            return model;
        }
        public ListBoxModel doFillArtifactItems(@QueryParameter String promotion, ArtifactType artifactType) {
            ListBoxModel model = new ListBoxModel();
        	System.out.println("Promotion = " + promotion);
        	if (artifactType != null) {
        		System.out.println("Type= " + artifactType.toString());
        	}
            model.add("Test", "Test");
/*        	if (environment == null ) {
        		System.out.println("environment is null"); 
        		return null;
        	}
            String token = TalendConfiguration.get().getCredentialsid();
            String region = TalendConfiguration.get().getRegion();
            ListBoxModel model = new ListBoxModel();

            TalendCredentials credentials = null;
        	WorkspaceService workspaceService = null;

        	credentials = new TalendBearerAuth(token);

    		workspaceService = WorkspaceService.instance(credentials, TalendCloudRegion.valueOf(region));
    		try {
    		Workspace[] workSpaces = workspaceService.get("query=environment.id=="+environment);
    		java.util.List<String> idList = new ArrayList<String>();  
            for (Workspace space : workSpaces) {
            		String id = space.getId();
            		String envId = space.getEnvironment().getId();
            		java.util.List<String> idFilter = idList
                            .stream()
                            .filter(x -> x.contains(envId))
                            .collect(Collectors.toList());
            		if (idFilter.size() == 0) {
            			model.add(space.getName() + " - " + space.getOwner(), id);
            		}
            		idList.add(id);
            	}
            
            } 
    		catch(TalendRestException | IOException ex){
    			System.out.println(ex.getMessage());
            	return null;
            }*/

            return model;
        }
        
    	
        public ListBoxModel doFillJobItems(@QueryParameter String environment, @QueryParameter String workspace) {
        	if (environment == null ) {
        		System.out.println("environment is null"); 
        		return null;
        	}
            String token = TalendConfiguration.get().getCredentialsid();
            String region = TalendConfiguration.get().getRegion();
            ListBoxModel model = new ListBoxModel();

            TalendCredentials credentials = null;
            ExecutableService executableService = null;

        	credentials = new TalendBearerAuth(token);

        	executableService = ExecutableService.instance(credentials, TalendCloudRegion.valueOf(region));
    		try {
    			Executable[] Executables = executableService.getByQuery("environmentId="+environment+"&workspaceId="+workspace);
    			for (Executable exec : Executables) {
            		model.add(exec.getName(), exec.getExecutable());
            	}          
            } 
    		catch(TalendRestException | IOException ex){
    			System.out.println(ex.getMessage());
            	return null;
            }

            return model;
        }
    	
        public FormValidation doCheckName(@QueryParameter String environment, @QueryParameter String workspace, @QueryParameter String job )
                throws IOException, ServletException {
            if (job.length() == 0)
                return FormValidation.warning("No Job");
            if (environment.length() < 4)
                return FormValidation.warning("No Env");
            if (workspace.length() < 4) {
                return FormValidation.warning("No Workspace");
            }
            return FormValidation.ok();
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
