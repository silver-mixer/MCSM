package com.github.silver_mixer.MCSM;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class JClosableTabbedPane extends JTabbedPane{
	private static final long serialVersionUID = 1L;
	private JClosableTabbedPane tabbedPane;

	public JClosableTabbedPane() {
		super();
		tabbedPane = this;
	}
	
	@Override
	public void addTab(String title, Component content) {
		addTab(title, null, content, false, null);
	}
	
	public void addTab(String title, String tooltip, Component content, boolean isClosable, Server parentServer) {
		JPanel tab = new JPanel(new BorderLayout());
		tab.setOpaque(false);
		JLabel label = new JLabel(title);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
		if(isClosable) {
			ImageIcon icon = new ImageIcon(this.getClass().getResource("assets/close.png"));
			JButton button = new JButton(icon);
			button.setPreferredSize(new Dimension(icon.getIconHeight(), icon.getIconWidth()));
			button.setContentAreaFilled(false);
			button.setBorderPainted(false);
			button.setFocusable(false);
			button.setBackground(Color.RED);
			button.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e){
					if(parentServer.isRunning()) {
						if(JOptionPane.showConfirmDialog(null, "サーバは動作中です。タブを閉じますか?\n(終了処理は行われます。)", "MCSM", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)return;
					}
					int index = tabbedPane.tabbedPane.indexOfTabComponent(tab);
					if(index != -1)tabbedPane.remove(index);
				}
				
				@Override
				public void mouseEntered(MouseEvent e){
					button.setContentAreaFilled(true);
				}
		
				@Override
				public void mouseExited(MouseEvent e){
					button.setContentAreaFilled(false);
				}
			});
			tab.add(button, BorderLayout.EAST);
		}
		tab.add(label, BorderLayout.WEST);
		tab.setBorder(BorderFactory.createEmptyBorder(2, 1, 1, 1));
		super.addTab(null, content);
		setTabComponentAt(getTabCount() - 1, tab);
		setToolTipTextAt(getTabCount() - 1, tooltip);
	}
}
