package io.jenkins.plugins.talend;

import hudson.Launcher;
import hudson.EnvVars;
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
import hudson.RelativePath;


import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.talend.tmc.dom.TaskNew;
import com.talend.tmc.dom.Trigger;
import com.talend.tmc.dom.Runtime;
import com.talend.tmc.dom.Artifact;
import com.talend.tmc.dom.Engine;
import com.talend.tmc.dom.RunConfig;
import com.talend.tmc.dom.Task;
import com.talend.tmc.dom.Workspace;
import com.talend.tmc.services.TalendBearerAuth;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendError;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.artifacts.ArtifactService;
import com.talend.tmc.services.executables.ExecutableRunConfig;
import com.talend.tmc.services.executables.ExecutableTask;
import com.talend.tmc.services.runtime.EngineService;
import com.talend.tmc.services.workspaces.WorkspaceService;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.kohsuke.stapler.AncestorInPath;

import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class CreateTaskBuilder extends Builder implements SimpleBuildStep {
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

    private String tEnvironment;
    private String tWorkspace;
    private String tArtifact;

    @DataBoundConstructor
    public CreateTaskBuilder() {
    	this.tArtifact = "";
    }	

    public String getEnvironment() {
        return tEnvironment;
    }

    @DataBoundSetter
    public void setEnvironment(String value) {
        this.tEnvironment = value;
    }

    public String getWorkspace() {
        return tWorkspace;
    }

    @DataBoundSetter
    public void setWorkspace(String value) {
        this.tWorkspace = value;
    }

    public String getArtifact() {
        return tArtifact;
    }

    @DataBoundSetter
    public void setArtifact(String value) {
        this.tArtifact = value;
    }
    
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("*** CREATETASK ***");
		listener.getLogger().println("artifactname=" + tArtifact);
		listener.getLogger().println("environment=" + tEnvironment);
		listener.getLogger().println("workspace=" + tWorkspace);
		listener.getLogger().println("*** CREATETASK ***");
        String id = "";

		TalendCredentials credentials = TalendLookupHelper.getTalendCredentials();
		TalendCloudRegion region = TalendLookupHelper.getTalendRegion();

		
        try {
            ExecutableTask executableTask = ExecutableTask.instance(credentials, region);
            ArtifactService artifactService = ArtifactService.instance(credentials, region);

    		String environmentId = TalendLookupHelper.getEnvironmentIdByName(tEnvironment);
            listener.getLogger().println("environmentId=" + environmentId);

            String workspaceId = TalendLookupHelper.getWorkspaceIdByName(environmentId, tWorkspace);
            listener.getLogger().println("workspaceId=" + workspaceId);

            Artifact[] artifacts = artifactService.getByName(tArtifact, workspaceId);
        	if (artifacts.length > 0) {
        		Artifact artifact = artifacts[0];
        		TaskNew newTask = new TaskNew();
        		newTask.setName(tArtifact);
        		newTask.setDescription("Jenkins created Task");
        		newTask.setWorkspaceId(workspaceId);
        		newTask.setEnvironmentId(environmentId);
        		
        		Map<String, String> artifactMap = new HashMap<>();
        		artifactMap.put("id", artifact.getId());
        		String[] versions = artifact.getVersions();
        		artifactMap.put("version", versions[0]);
        		newTask.setArtifact(artifactMap);

        		Map<String, String> AutoUpgradeInfoMap = new HashMap<>();
        		AutoUpgradeInfoMap.put("autoUpgradable", "true");
        		AutoUpgradeInfoMap.put("overrideWithDefaultParameters", "false");
        		newTask.setAutoUpgradeInfo(AutoUpgradeInfoMap);

        		Task createdTask = executableTask.create(newTask);

        		id = createdTask.getId();
            	listener.getLogger().println("New Task has Id =" + id);
            	listener.getLogger().println("Going to add RunConfig with default values to the task");
            	listener.getLogger().println("The trigger is MANUAL and we take the first Remote engine in the workspace");

            	String engineId = TalendLookupHelper.getFirstEngineId(environmentId);
            	if (!engineId.isEmpty()) {
                	RunConfig runConfig = new RunConfig();
                	Trigger trigger = new Trigger();
                	trigger.setType("MANUAL");
                	runConfig.setTrigger(trigger);
                	Runtime runtime = new Runtime();
                	runtime.setId(engineId);
                	runtime.setType("REMOTE_ENGINE");
                	runConfig.setRuntime(runtime);
                	// This can only be set on Clusters
//	                	runConfig.setParallelExecutionAllowed("false");
                    ExecutableRunConfig executableRunConfig = ExecutableRunConfig.instance(credentials, region);
                    executableRunConfig.update("task", id, runConfig);
            	} else {
                	listener.getLogger().println("No Engine in Workspace available, skipping updating RunConfig");            		
            	}
            	
            	listener.getLogger().println("The TaskID is stored in Environment variable TALEND_NEW_TASK_ID ");
            	env.put("TALEND_NEW_TASK_ID", id);
        	} else if (artifacts.length == 0) {
            	listener.getLogger().println("Artifact " + tArtifact + " Not Found");
        	}
            Thread.sleep(10);  // to include the InterruptedException
        } catch(TalendRestException | IOException | InterruptedException ex){
        	listener.getLogger().println(ex.getMessage());
        	
        }
          catch(Exception e) {
        	  listener.getLogger().println(e.getMessage());
          }
    }

    @Symbol("createTask")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
    	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

		public ListBoxModel doFillEnvironmentItems(@CheckForNull @AncestorInPath Item context) {
			return TalendLookupHelper.getEnvironmentList();
		}

		public ListBoxModel doFillWorkspaceItems(@QueryParameter String environment) {
			return TalendLookupHelper.getWorkspaceList(environment);
		}
            	    	
        public FormValidation doCheckName(@QueryParameter String environment, @QueryParameter String workspace, @QueryParameter String artifactname )
                throws IOException, ServletException {
            if (artifactname.length() == 0)
                return FormValidation.warning("No Artifactname");
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
            return Messages.CreateTaskBuilder_DescriptorImpl_DisplayName();
        }

    }

}
