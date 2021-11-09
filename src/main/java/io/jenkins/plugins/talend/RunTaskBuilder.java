package io.jenkins.plugins.talend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.talend.tmc.dom.Executable;
import com.talend.tmc.dom.ExecutionRequest;
import com.talend.tmc.dom.ExecutionResponse;
import com.talend.tmc.dom.Task;
import com.talend.tmc.dom.Workspace;
import com.talend.tmc.services.TalendBearerAuth;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.executables.ExecutableService;
import com.talend.tmc.services.executables.ExecutableTask;
import com.talend.tmc.services.executions.ExecutionService;
import com.talend.tmc.services.workspaces.WorkspaceService;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
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
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

	private String tEnvironment;
	private String tWorkspace;
	private String tTaskid;
	private String tJobname;

	@DataBoundConstructor
	public RunTaskBuilder() {
		this.tTaskid = "";
		this.tJobname = "";
	}

	public String getEnvironment() {
		return tEnvironment;
	}

	@DataBoundSetter
	public void setEnvironment(String environment) {
		this.tEnvironment = environment;
	}

	public String getWorkspace() {
		return tWorkspace;
	}

	@DataBoundSetter
	public void setWorkspace(String workspace) {
		this.tWorkspace = workspace;
	}

	public String getTaskid() {
		return tTaskid;
	}

	@DataBoundSetter
	public void setTaskid(String value) {
		this.tTaskid = value;
	}

	public String getJobname() {
		return tJobname;
	}

	@DataBoundSetter
	public void setJobname(String value) {
		this.tJobname = value;
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		String token = TalendConfiguration.get().getCredentialsid();
		String region = TalendConfiguration.get().getRegion();
		String id = "";
		Task task = null;

        StringCredentials stringCredentials = CredentialsHelper.lookupSystemCredentials(token);

		String api = stringCredentials.getSecret().getPlainText();
        TalendCredentials credentials = new TalendBearerAuth(api);

        listener.getLogger().println("region=" + region);
		listener.getLogger().println("taskid=" + tTaskid);
		listener.getLogger().println("jobname=" + tJobname);
		listener.getLogger().println("environment=" + tEnvironment);
		listener.getLogger().println("workspace=" + tWorkspace);

		try {
			ExecutionService executionService = ExecutionService.instance(credentials,
					TalendCloudRegion.valueOf(region));

			ExecutableTask executableTask = ExecutableTask.instance(credentials, TalendCloudRegion.valueOf(region));
			if (!tTaskid.isEmpty()) {
				task = executableTask.getById(tTaskid);
				id = task.getId();
			} else if (tJobname.isEmpty()) {
				WorkspaceService workspaceService = WorkspaceService.instance(credentials,
						TalendCloudRegion.valueOf(region));
				SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
				String query = fiql.is("name").equalTo(tWorkspace).and().is("environment.name").equalTo(tEnvironment)
						.query();
				Workspace[] workspaces = workspaceService.get(query);
				if (workspaces.length > 1)
					listener.getLogger()
							.println("More than 1 workspace returned with that name! We'll take the first one.");
				String workspaceId = workspaces[0].getId();
				listener.getLogger().println("workspaceId=" + workspaceId);
				Task[] tasks = null;
				tasks = executableTask.getByName(tJobname, workspaceId);
				task = tasks[0];
				listener.getLogger().println("task=" + task.getName());
				id = task.getExecutable();

			} else {
				listener.getLogger().println("No Taskid or Jobname provided!");
			}

			listener.getLogger().println("Found Task: " + id);

			ExecutionRequest executionRequest = new ExecutionRequest();
			executionRequest.setExecutable(id);

			/*
			 * TODO: Read all the logs until finished
			 * 
			 * Hashtable<String, String> parameters = null; if (Cli.hasCliValue("cv")) {
			 * String[] pairs = Cli.getCliValue("cv").split(";"); parameters = new
			 * Hashtable<>(); for (String pair : pairs) { String[] nv = pair.split("=");
			 * parameters.put(nv[0], nv[1]); }
			 * 
			 * executionRequest.setParameters(parameters); }
			 * 
			 */
			ExecutionResponse executionResponse = executionService.post(executionRequest);
			listener.getLogger().println("Talend Job Started: " + executionResponse.getExecutionId());

			Thread.sleep(10); // to include the InterruptedException
		} catch (TalendRestException | IOException | InterruptedException ex) {
			listener.getLogger().println(ex.getMessage());

		} catch (Exception e) {
			listener.getLogger().println(e.getMessage());
		}

	}

	@Extension
	@Symbol("runTask")
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public ListBoxModel doFillEnvironmentItems(@CheckForNull @AncestorInPath Item context) {
			String token = TalendConfiguration.get().getCredentialsid();
			String region = TalendConfiguration.get().getRegion();
			ListBoxModel model = new ListBoxModel();

			WorkspaceService workspaceService = null;

	        StringCredentials stringCredentials = CredentialsHelper.lookupSystemCredentials(token);

			String api = stringCredentials.getSecret().getPlainText();
	        TalendCredentials credentials = new TalendBearerAuth(api);

			workspaceService = WorkspaceService.instance(credentials, TalendCloudRegion.valueOf(region));
			try {
				Workspace[] workSpaces = workspaceService.get();
				java.util.List<String> idList = new ArrayList<String>();
				for (Workspace space : workSpaces) {
					String id = space.getEnvironment().getId();
					java.util.List<String> idFilter = idList.stream().filter(x -> x.equals(id))
							.collect(Collectors.toList());
					if (idFilter.size() == 0) {
						System.out.println("environment name = " + space.getEnvironment().getName());
						System.out.println("environment id = " + id);
						model.add(space.getEnvironment().getName(), id);
						idList.add(id);
					}
				}
			} catch (TalendRestException | IOException ex) {
				System.out.println(ex.getMessage());
				return null;
			}

			return model;
		}

		public ListBoxModel doFillWorkspaceItems(@QueryParameter String environment) {
			if (environment == null) {
				System.out.println("environment is null");
				return null;
			}
			String token = TalendConfiguration.get().getCredentialsid();
			String region = TalendConfiguration.get().getRegion();
			ListBoxModel model = new ListBoxModel();

			WorkspaceService workspaceService = null;

	        StringCredentials stringCredentials = CredentialsHelper.lookupSystemCredentials(token);

			String api = stringCredentials.getSecret().getPlainText();
	        TalendCredentials credentials = new TalendBearerAuth(api);

			workspaceService = WorkspaceService.instance(credentials, TalendCloudRegion.valueOf(region));
			try {
				SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
				String query = fiql.is("environment.id").equalTo(environment).query();
				Workspace[] workSpaces = workspaceService.get(query);
				java.util.List<String> idList = new ArrayList<String>();
				for (Workspace space : workSpaces) {
					String id = space.getId();
					String envId = space.getEnvironment().getId();
					java.util.List<String> idFilter = idList.stream().filter(x -> x.contains(envId))
							.collect(Collectors.toList());
					if (idFilter.size() == 0) {
						model.add(space.getName() + " - " + space.getOwner(), id);
					}
					idList.add(id);
				}

			} catch (TalendRestException | IOException ex) {
				System.out.println(ex.getMessage());
				return null;
			}

			return model;
		}

		public ListBoxModel doFillTaskidItems(@QueryParameter String environment, @QueryParameter String workspace) {
			if (environment == null) {
				System.out.println("environment is null");
				return null;
			}
			String token = TalendConfiguration.get().getCredentialsid();
			String region = TalendConfiguration.get().getRegion();
			ListBoxModel model = new ListBoxModel();

			ExecutableService executableService = null;

	        StringCredentials stringCredentials = CredentialsHelper.lookupSystemCredentials(token);

			String api = stringCredentials.getSecret().getPlainText();
	        TalendCredentials credentials = new TalendBearerAuth(api);

			executableService = ExecutableService.instance(credentials, TalendCloudRegion.valueOf(region));
			try {
				Executable[] Executables = executableService
						.getByQuery("environmentId=" + environment + "&workspaceId=" + workspace);
				for (Executable exec : Executables) {
					model.add(exec.getName(), exec.getExecutable());
				}
			} catch (TalendRestException | IOException ex) {
				System.out.println(ex.getMessage());
				return null;
			}

			return model;
		}

		public FormValidation doCheckName(@QueryParameter String environment, @QueryParameter String workspace,
				@QueryParameter String task) throws IOException, ServletException {
			if (task.length() == 0)
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
			return Messages.RunTaskBuilder_DescriptorImpl_DisplayName();
		}

	}

}
