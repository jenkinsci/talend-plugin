 package io.jenkins.plugins.talend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import com.talend.tmc.dom.Execution;
import com.talend.tmc.dom.ExecutionRequest;
import com.talend.tmc.dom.ExecutionResponse;
import com.talend.tmc.dom.Task;
import com.talend.tmc.dom.Workspace;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.executables.ExecutableTask;
import com.talend.tmc.services.executions.ExecutionService;
import com.talend.tmc.services.workspaces.WorkspaceService;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;

@Extension
public class RunTaskBuilder extends Builder implements SimpleBuildStep {
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private String tEnvironment;
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private String tWorkspace;
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private String tTask;
	private String tParameters = "";


	@DataBoundConstructor
	public RunTaskBuilder() {
		this.tTask = "";
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

	public String getTask() {
		return tTask;
	}

	@DataBoundSetter
	public void setTask(String value) {
		this.tTask = value;
	}

    public String getParameters () {
        return this.tParameters;
    }

    @DataBoundSetter
    public void setParameters(String value) {
    	tParameters = value;
    }

    @Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		listener.getLogger().println("*** RUNTASK ***");
		listener.getLogger().println("environment=" + tEnvironment);
		listener.getLogger().println("workspace=" + tWorkspace);
		listener.getLogger().println("jobname=" + tTask);
		listener.getLogger().println("parameters=" + tParameters);
		String id = "";

		TalendCredentials credentials = TalendLookupHelper.getTalendCredentials();
		TalendCloudRegion region = TalendLookupHelper.getTalendRegion();

		try {

			if (!tTask.isEmpty()) {
				id = TalendLookupHelper.getTaskIdByName(tEnvironment, tWorkspace, tTask);

			} else {
				throw new Exception("No Task provided!");
			}

			listener.getLogger().println("Found Task with id: " + id);

			ExecutionRequest executionRequest = new ExecutionRequest();
    		listener.getLogger().println("*** 4  RUNTASK ***");
			executionRequest.setExecutable(id);
    		listener.getLogger().println("*** 3  RUNTASK ***");
			
			String[] values = tParameters.split("\n");
			Map<String, String> parameters = new HashMap<>();
			for (int i = 0; i < values.length; i++) {
				if (!(values[i].indexOf("=") < 0) ) {
					String key =values[i].split("=")[0].trim();
					String value =values[i].split("=")[1].trim();
					if (key.length() > 0 && value.length() > 0) {
						parameters.put(key, value);
					}
				}
			}
			if (parameters.size() > 0) {
				executionRequest.setParameters(parameters);
			}

			/*
			 * TODO: Read all the logs until finished
			 * 
			 */
    		listener.getLogger().println("*** 2  RUNTASK ***");
			ExecutionService executionService = ExecutionService.instance(credentials, region);
    		listener.getLogger().println("*** 1  RUNTASK ***");
			ExecutionResponse executionResponse = executionService.post(executionRequest);
			listener.getLogger().println("Talend Task Started: " + executionResponse.getExecutionId());
            while (true) {

                Execution execution = executionService.get(executionResponse.getExecutionId());
                if (execution.getFinishTimestamp() != null) {
                    if (!execution.getExecutionStatus().equals("EXECUTION_SUCCESS")) {
                        throw new InterruptedException("Job Completed in non Successful State :" + execution.toString());
                    } else {
                    	LOGGER.info("Job Finished Succesfully");
                    }
                    break;
                } else {
                    Thread.sleep(5000);
                }
            }
    		listener.getLogger().println("*** RUNTASK ***");

			Thread.sleep(10); // to include the InterruptedException
		}
		 catch(IOException ex){
		    	String message = ex.getMessage();
		    	listener.getLogger().println("**** ERROR2 ****");
		    	listener.getLogger().println(message);
		    	run.setResult(Result.FAILURE);
		    	listener.getLogger().println("**** ERROR2 ****");
		    	throw new InterruptedException (message);
		    
		        }
		catch(TalendRestException ex){
        	String message = ex.getMessage();
        	listener.getLogger().println("**** ERROR1 ****");
        	listener.getLogger().println(message);
        	run.setResult(Result.FAILURE);
        	listener.getLogger().println("**** ERROR1 ****");
        	throw new InterruptedException (message);
        
    } catch(NullPointerException ex){
        	String message = ex.getMessage();
        	listener.getLogger().println("**** ERROR3 ****");
        	listener.getLogger().println(message);
        	run.setResult(Result.FAILURE);
        	listener.getLogger().println("**** ERROR3 ****");
        	throw new InterruptedException (message);
        }
	 catch (RuntimeException e) {
		throw e;
    }          catch(Exception e) {
        	listener.getLogger().println(e.getMessage());
        	run.setResult(Result.FAILURE);
          }

	}

	@Extension
	@Symbol("runTask")
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @POST
		public ListBoxModel doFillEnvironmentItems(@CheckForNull @AncestorInPath Item item) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
			return TalendLookupHelper.getEnvironmentList();
		}

        @POST
		public ListBoxModel doFillWorkspaceItems(@AncestorInPath Item item,@QueryParameter String environment) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	if (!environment.isEmpty()) {
        		return TalendLookupHelper.getWorkspaceList(environment);
        	}
        	return model;
		}

        @POST
		public ListBoxModel doFillTaskItems(@AncestorInPath Item item, @QueryParameter String environment, @QueryParameter String workspace) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	if (!environment.isEmpty() && !workspace.isEmpty()) {
        		return TalendLookupHelper.getTaskList(environment, workspace);
        	}
        	return model;
		}

        @POST
        public FormValidation doCheckParameters(@AncestorInPath Item item, @QueryParameter String parameters)
			throws IOException, ServletException {
            if (item == null) { // no context
                return FormValidation.error("No context");
            }
            item.checkPermission(Item.CONFIGURE);
			if (!parameters.isEmpty() && (parameters.indexOf("=") < 0)) {
				return FormValidation.warning("Invalid Parameters");
			}
			if (!parameters.isEmpty() && (parameters.indexOf("=") == 0)) {
				return FormValidation.warning("Invalid Parameters");
			}
			String[] values = parameters.split("\n");
			for (int i = 0; i < values.length; i++) {
				if (!(values[i].indexOf("=") < 0)) {
					String key =values[i].split("=")[0].trim();
					if (!key.matches("[a-zA-Z0-9_]+")) {
						return FormValidation.warning("Keys may only contain characters, numbers and underscores: '" + key + "'");
					}
				}
			}
			return FormValidation.ok();
        }
        
		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.RunTaskBuilder_DescriptorImpl_DisplayName();
		}

	}

}
