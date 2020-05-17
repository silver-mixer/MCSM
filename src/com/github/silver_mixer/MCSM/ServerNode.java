package com.github.silver_mixer.MCSM;

import java.util.Vector;

public class ServerNode extends Vector<Object>{
	private static final long serialVersionUID = 1L;
	private String name;
	
	public ServerNode(String name) {
		super();
		this.name = name;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
	public ServerNode getServerNode(String name) {
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i) instanceof ServerNode && this.get(i).toString().equals(name))return (ServerNode)this.get(i);
		}
		return null;
	}
}