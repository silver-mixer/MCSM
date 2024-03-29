package com.github.silver_mixer.MCSM;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

class Server{
	enum ServerOptions{
		LAUNCH_COMMAND("起動コマンド変更"),
		JVM_ARGUMENTS("JVM引数変更"),
		SERVER_ARGUMENTS("サーバー引数変更"),
		STOP_COMMAND("終了コマンド変更"),
		SET_SERVER_JAR("jarファイル設定");
		
		private String text;
		
		private ServerOptions(String text){
			this.text = text;
		}
		
		public String getText(){
			return this.text;
		}
		
		public static String[] getTexts() {
			ServerOptions[] serverOptions = values();
			String[] texts = new String[serverOptions.length];
			for(int i = 0; i < serverOptions.length; i++) {
				texts[i] = serverOptions[i].getText();
			}
			return texts;
		}
	}
	
	private static List<Server> servers = new ArrayList<Server>();
	private static final int LOG_LIMIT = 1000;
	private boolean isRunning = false;
	private Server server;
	private String name;
	private String profilePath;
	private Properties profile;
	private JClosableTabbedPane tab;
	private JPanel serverPanel;
	private JTextArea textArea;
	private JTextField commandArea;
	private File serverFile = null;
	private PrintStream printStream = null;
	private BufferedReader bufferedReader = null;
	private String launchCommand = "java", jvmArguments = "", arguments = "", stopCommand = "stop";
	
	public Server(JClosableTabbedPane tab, String profilePath) {
		this.tab = tab;
		this.name = profilePath.substring((profilePath.contains("/") ? profilePath.lastIndexOf("/") + 1 : 0), profilePath.lastIndexOf(".dat"));
		this.profilePath = profilePath;
		this.server = this;
		servers.add(this);
		
		serverPanel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		serverPanel.setLayout(layout);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		layout.setConstraints(scrollPane, gbc);
		serverPanel.add(scrollPane);
		textArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e){
				if((scrollPane.getVerticalScrollBar().getValue() + scrollPane.getVerticalScrollBar().getHeight()) == scrollPane.getVerticalScrollBar().getMaximum()) {
					textArea.setCaretPosition(textArea.getText().length());
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e){}

			@Override
			public void changedUpdate(DocumentEvent e){}
		});
		
		commandArea = new JTextField();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		layout.setConstraints(commandArea, gbc);
		commandArea.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(printlnOutput(commandArea.getText()))commandArea.setText("");
			}
		});
		serverPanel.add(commandArea);
		
		JPanel controlPanel = new JPanel();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		gbc.gridheight = 2;
		controlPanel.setPreferredSize(new Dimension(150, 0));
		controlPanel.setMinimumSize(new Dimension(150, 0));
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		layout.setConstraints(controlPanel, gbc);
		serverPanel.add(controlPanel);
		
		JButton startButton = new JButton("開始");
		startButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, startButton.getMaximumSize().height));
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				start();
			}
		});
		
		JButton stopButton = new JButton("終了");
		stopButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, stopButton.getMaximumSize().height));
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				printlnOutput(stopCommand);
			}
		});
		
		JButton clearButton = new JButton("ログ削除");
		clearButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, clearButton.getMaximumSize().height));
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				textArea.setText("");
			}
		});
		
		JButton optionButton = new JButton("設定");
		optionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, optionButton.getMaximumSize().height));
		optionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				int answer = JOptionPane.showOptionDialog(MCSM.getMCSM(), "設定する項目を選んでください。", "MCSM", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, ServerOptions.getTexts(), ServerOptions.JVM_ARGUMENTS.getText());
				if(0 <= answer && answer < ServerOptions.values().length) {
					ServerOptions serverOption = ServerOptions.values()[answer];
					if(serverOption == ServerOptions.LAUNCH_COMMAND) {
						String newLaunchCommand = JOptionPane.showInputDialog(MCSM.getMCSM(), "起動コマンドを入力してください。", launchCommand);
						if(newLaunchCommand != null) {
							if(newLaunchCommand.isEmpty() || newLaunchCommand.equals("java")) {
								launchCommand = "java";
								setProfileValue("launch_command", null);
							}else {
								launchCommand = newLaunchCommand;
								setProfileValue("launch_command", launchCommand);
							}
							textArea.append("[MCSM INFO]: Launch command set to: " + launchCommand + System.lineSeparator());
						}
					}else if(serverOption == ServerOptions.JVM_ARGUMENTS) {
						String newJvmArguments = JOptionPane.showInputDialog(MCSM.getMCSM(), "JVM引数を入力してください。", jvmArguments);
						if(newJvmArguments != null) {
							jvmArguments = newJvmArguments;
							textArea.append("[MCSM INFO]: JVM arguments set to: " + jvmArguments + System.lineSeparator());
							if(!jvmArguments.isEmpty()) {
								setProfileValue("jvm_arguments", jvmArguments);
							}else {
								setProfileValue("jvm_arguments", null);
							}
						}
					}else if(serverOption == ServerOptions.SERVER_ARGUMENTS) {
						String newArguments = JOptionPane.showInputDialog(MCSM.getMCSM(), "サーバー引数を入力してください。", arguments);
						if(newArguments != null) {
							arguments = newArguments;
							textArea.append("[MCSM INFO]: Server arguments set to: " + arguments + System.lineSeparator());
							if(!arguments.isEmpty()) {
								setProfileValue("arguments", arguments);
							}else {
								setProfileValue("arguments", null);
							}
						}
					}else if(serverOption == ServerOptions.STOP_COMMAND) {
						String newStopCommand = JOptionPane.showInputDialog(MCSM.getMCSM(), "終了コマンドを入力してください。", stopCommand);
						if(newStopCommand != null) {
							if(newStopCommand.isEmpty() || newStopCommand.equals("stop")) {
								stopCommand = "stop";
								setProfileValue("stop_command", null);
							}else {
								stopCommand = newStopCommand;
								setProfileValue("stop_command", stopCommand);
							}
							textArea.append("[MCSM INFO]: Server stop command set to: " + stopCommand + System.lineSeparator());
						}
					}else if(serverOption == ServerOptions.SET_SERVER_JAR) {
						JFileChooser fileChooser = new JFileChooser();
						fileChooser.showOpenDialog(null);
						if(fileChooser.getSelectedFile() != null){
							serverFile = fileChooser.getSelectedFile();
							setProfileValue("jarfile", serverFile.getAbsolutePath());
							textArea.append("[MCSM INFO]: jarfile set to: " + serverFile.getAbsolutePath() + System.lineSeparator());
						}
					}
				}
			}
		});
		
		JButton fileSelectButton = new JButton("jarフォルダを開く");
		fileSelectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, fileSelectButton.getMaximumSize().height));
		fileSelectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(Desktop.isDesktopSupported() && serverFile != null) {
					try {
						Desktop.getDesktop().open(serverFile.getParentFile());
					}catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		controlPanel.add(startButton);
		controlPanel.add(stopButton);
		controlPanel.add(Box.createGlue());
		controlPanel.add(clearButton);
		controlPanel.add(optionButton);
		controlPanel.add(fileSelectButton);
		
		tab.addTab(name, "servers/" + profilePath, serverPanel, true, this);
		tab.setSelectedIndex(tab.getTabCount() - 1);

		serverPanel.addHierarchyListener(new HierarchyListener() {
			@Override
			public void hierarchyChanged(HierarchyEvent event){
				if(event.getChangeFlags() == HierarchyEvent.PARENT_CHANGED) {
					remove();
				}
			}
		});

		if(!loadProfile())JOptionPane.showMessageDialog(null, "プロファイルの生成、読み込みに失敗しました。\n設定は保存されません。(発生: \"" + name + "\")", "MCSM - エラー", JOptionPane.ERROR_MESSAGE);
		if(profile.containsKey("jarfile")) {
			File tempServerFile = new File(profile.getProperty("jarfile"));
			if(tempServerFile.exists()) {
				serverFile = tempServerFile;
				textArea.append("[MCSM INFO]: jarfile was loaded: " + serverFile.getAbsolutePath() + System.lineSeparator());
			}
		}
		if(profile.containsKey("launch_command")) {
			launchCommand = profile.getProperty("launch_command");
			textArea.append("[MCSM INFO]: Launch command was loaded: " + launchCommand + System.lineSeparator());
		}
		if(profile.containsKey("jvm_arguments")) {
			jvmArguments = profile.getProperty("jvm_arguments");
			textArea.append("[MCSM INFO]: JVM arguments was loaded: " + jvmArguments + System.lineSeparator());
		}
		if(profile.containsKey("arguments")) {
			arguments = profile.getProperty("arguments");
			textArea.append("[MCSM INFO]: Server arguments was loaded: " + arguments + System.lineSeparator());
		}
		if(profile.containsKey("stop_command")) {
			stopCommand = profile.getProperty("stop_command");
			textArea.append("[MCSM INFO]: Server stop command was loaded: " + stopCommand + System.lineSeparator());
		}
	}
	
	public static List<Server> getServers(){
		return servers;
	}
	
	public static Server getServer(String name) {
		for(Server server: servers) {
			if(server.getUniqueName().equals(name))return server;
		}
		return null;
	}
	
//	public String getName() {
//		return name;
//	}
	
	public String getUniqueName() {
		return profilePath.substring(0, profilePath.lastIndexOf(".dat"));
	}
	
	public void start() {
		if(serverFile != null && serverFile.exists() && !isRunning) {
			try{
				isRunning = true;
				textArea.append("[MCSM INFO]: Starting server" + System.lineSeparator());
				List<String> command = new ArrayList<String>();
				command.add(launchCommand);
				for(String arg: jvmArguments.split(" "))if(!arg.isEmpty())command.add(arg);
				command.addAll(Arrays.asList("-jar", serverFile.getAbsolutePath()));
				for(String arg: arguments.split(" "))if(!arg.isEmpty())command.add(arg);
				ProcessBuilder processBuilder = new ProcessBuilder(command);
				processBuilder.directory(serverFile.getParentFile());
				processBuilder.redirectErrorStream(true);
				Process process = processBuilder.start();
				bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				printStream = new PrintStream(process.getOutputStream());
				SwingWorker<Void, String> worker = new SwingWorker<Void, String>(){
					@Override
					protected Void doInBackground() throws Exception{
						String line = bufferedReader.readLine();
						while(line != null) {
							publish(line);
							line = bufferedReader.readLine();
						}
						return null;
					}
					
					@Override
					protected void process(List<String> lines) {
						if(lines.size() > LOG_LIMIT) {
							textArea.setText("");
							for(int i = lines.size() - LOG_LIMIT; i < lines.size(); i++) {
								textArea.append(lines.get(i));
								textArea.append(System.lineSeparator());
							}
						}else {
							if(textArea.getLineCount() + lines.size() > LOG_LIMIT) {
								int deletePosition = StringUtils.ordinalIndexOf(textArea.getText(), "\n", (textArea.getLineCount() + lines.size()) - LOG_LIMIT - 1);
								if(deletePosition != -1)textArea.replaceRange("", 0, deletePosition + 1);
							}
							for(String line: lines) {
								textArea.append(line);
								textArea.append(System.lineSeparator());
							}
						}
					}
					
					@Override
					protected void done() {
						textArea.append("[MCSM INFO]: Server has been closed" + System.lineSeparator());
						close();
					}
				};
				worker.execute();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	public static boolean existLaunchServer(String name) {
		for(Server server: servers) {
			if(server.getUniqueName().equals(name))return true;
		}
		return false;
	}
	
	public static String backupServer(String name) {
		if(!MCSM.existServer(name))return "Does not exist server.";
		Properties profile = new Properties();
		File file = new File(MCSM.getServersDirectory(), name + ".dat");
		if(file.exists()) {
			try {
				profile.load(new FileInputStream(file));
			}catch(IOException e) {
				e.printStackTrace();
				return "Does not exist server profile.";
			}
		}
		if(!profile.containsKey("jarfile"))return "jarfile directory is not set.";
		File serverFolder = new File(profile.getProperty("jarfile")).getParentFile();
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS");
		ProcessBuilder pb = new ProcessBuilder();
		String backupFileName = sdf.format(calendar.getTime()) + ".7z";
		pb.command("7z", "a", MCSM.getBackupsDirectory().getAbsolutePath() + "/" + name + "/" + backupFileName, serverFolder.getAbsolutePath());
		pb.redirectErrorStream(true);
		Process process = null;
		try {
			process = pb.start();
			InputStream is = process.getInputStream();
			while(is.read() != -1);
			process.waitFor();
		}catch(Exception e) {
			e.printStackTrace();
		}
		if(process != null && process.exitValue() == 0) {
			return "Backup \"" + backupFileName + "\" has been generated.";
		}
		return "An error has been occurred.";
	}
	
	public static String restoreServer(String name, String backup) {
		if(!MCSM.existServer(name))return "Does not exist server.";
		if(existLaunchServer(name))return "Server is running.";
		if(!new File(MCSM.getBackupsDirectory(), "/" + name + "/" + backup).exists())return "Backup \"" + backup + "\" does not exist.";
		Properties profile = new Properties();
		File file = new File(MCSM.getServersDirectory(), name + ".dat");
		if(file.exists()) {
			try {
				profile.load(new FileInputStream(file));
			}catch(IOException e) {
				e.printStackTrace();
				return "Does not exist server profile.";
			}
		}
		if(!profile.containsKey("jarfile"))return "jarfile directory is not set.";
		File serverFolder = new File(profile.getProperty("jarfile")).getParentFile();
		File serverParentFolder = serverFolder.getParentFile();
		try {
			FileUtils.deleteDirectory(serverFolder);
		}catch(Exception e) {
			return "Server delete error.";
		}
		ProcessBuilder pb = new ProcessBuilder();
		pb.command("7z", "x", "-o" + serverParentFolder.getAbsolutePath(), MCSM.getBackupsDirectory().getAbsolutePath() + "/" + name + "/" + backup);
		pb.redirectErrorStream(true);
		Process process = null;
		try {
			process = pb.start();
			InputStream is = process.getInputStream();
			while(is.read() != -1);
			process.waitFor();
		}catch(Exception e) {
			e.printStackTrace();
		}
		if(process != null && process.exitValue() == 0) {
			return "Backup \"" + backup + "\" has been restored.";
		}
		return "An error has been occurred.";
	}
	
	public static String getServerBackupList(String name) {
		File serverBackupFolder = new File(MCSM.getBackupsDirectory().getAbsolutePath() + "/" + name);
		if(!serverBackupFolder.exists())return "Backup folder does not exist";
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File directory, String name) {
				return name.endsWith(".7z");
			}
		};
		String backups[] = serverBackupFolder.list(filter);
		if(backups == null) {
			return "Backup does not exist.";
		}else {
			return "[\"" + name + "\"'s backup_id list]\n" + String.join("\n", backups);
		}
	}
	
	public boolean loadProfile() {
		profile = new Properties();
		File file = new File(MCSM.getServersDirectory(), profilePath);
		if(file.exists()) {
			try {
				profile.load(new FileInputStream(file));
			}catch(IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	public void setProfileValue(String key, String value) {
		if(value != null) {
			profile.setProperty(key, value);
		}else {
			profile.remove(key);
		}
		try {
			profile.store(new FileOutputStream(new File(MCSM.getServersDirectory(), profilePath)), null);
		}catch(IOException e) {}
	}
	
	public boolean printlnOutput(String string) {
		if(printStream == null)return false;
		printStream.println(string);
		printStream.flush();
		return true;
	}
	
	public void close() {
		if(printStream != null) {
			printlnOutput(stopCommand);
			printStream.close();
		}
		try {
			if(bufferedReader != null)bufferedReader.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
		isRunning = false;
	}
	
	public void remove() {
		close();
		servers.remove(server);
		tab.remove(serverPanel);
	}
	
	public boolean isRunning() {
		return isRunning;
	}
}