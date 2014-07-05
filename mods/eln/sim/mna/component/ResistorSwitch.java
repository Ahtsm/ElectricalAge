package mods.eln.sim.mna.component;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;
import mods.eln.sim.mna.state.State;

public class ResistorSwitch extends Resistor implements INBTTReady{
	public ResistorSwitch() {
	}
	
	public ResistorSwitch(State aPin,State bPin) {
		super(aPin, bPin);
	}
	
	boolean state = false;
	
	public void setState(boolean state){
		this.state = state;
		setR(baseR);
	}
	
	protected double baseR = 1;
	@Override
	public Resistor setR(double r) {
		baseR = r;
		return super.setR(state ? r : 1000000000.0);
	}
	
	
	public boolean getState() {
		// TODO Auto-generated method stub
		return state;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setR(nbt.getDouble(str + "R"));
		if(Double.isNaN(baseR)) highImpedance();
		setState(nbt.getBoolean(str + "State"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "R",baseR);
		nbt.setBoolean(str + "State",getState());
	}
}