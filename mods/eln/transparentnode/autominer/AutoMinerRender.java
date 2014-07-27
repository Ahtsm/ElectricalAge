package mods.eln.transparentnode.autominer;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.client.FrameTime;
import mods.eln.item.electricalitem.PortableOreScannerItem.RenderStorage;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class AutoMinerRender extends TransparentNodeElementRender {
	AutoMinerDescriptor descriptor;
	float[] buttonsState;
	boolean[] ledsAState;
	boolean[] ledsPState;

	public AutoMinerRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (AutoMinerDescriptor) descriptor;

		buttonsState = new float[this.descriptor.buttonsCount];
		for (int idx = 0; idx < this.descriptor.buttonsCount; idx++) {
			buttonsState[idx] = (float) Math.random();
		}

		ledsAState = new boolean[this.descriptor.ledsACount];
		for (int idx = 0; idx < this.descriptor.ledsACount; idx++) {
			ledsAState[idx] = Math.random() > 0.5;
		}

		ledsPState = new boolean[this.descriptor.ledsPCount];
		for (int idx = 0; idx < this.descriptor.ledsPCount; idx++) {
			ledsPState[idx] = Math.random() > 0.5;
		}
	}

	int logSizeMax = 9;
	LinkedList<String> logs = new LinkedList<String>();
	private void pushLog(String string) {
		logs.addFirst(string);
		if(logs.size() > logSizeMax)
			logs.removeLast();
	}

	RenderStorage render = new RenderStorage(10, 130, 24, 24);
	
	@Override
	public void draw() {

		if (pipeLength != 0) {
			GL11.glPushMatrix();
			for (int idx = pipeLength; idx != 0; idx--) {
				if (idx != 1) {
					descriptor.pipe.draw();
				}
				else {
					descriptor.head.draw();
				}
				GL11.glTranslatef(0, -1f, 0);
			}
			GL11.glPopMatrix();

		}

		for (int idx = 0; idx < this.descriptor.buttonsCount; idx++) {
			buttonsState[idx] = idx == job.ordinal() && powerOk ? 1 : 0;
		}

		front.glRotateXnRef();

		boolean drawScreen = UtilsClient.clientDistanceTo(tileEntity) < 20 && powerOk;
		boolean drawRay = drawScreen && job != null;

		UtilsClient.disableCulling();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-1.57031f, 1.8125f - 1.5f - 0.02f, -0.3125f + 0.02f);
		GL11.glRotatef(90, 0, 1, 0);

		if(drawScreen){
			UtilsClient.disableLight();
			GL11.glPushMatrix();
			GL11.glRotatef(180, 0, 1, 0);
			GL11.glScalef(1/128f, -1/128f, 1);
			int idx = 0;
			for(String log : logs){
				Minecraft.getMinecraft().fontRenderer.drawString(idx == 0 ? "> " + log : log, 80,1+idx, 0xFFD0D0D0);
				idx += 8;
			}
			GL11.glPopMatrix();
			UtilsClient.enableLight();
		}

		if (drawRay) {

			float raySize = 0.625f - 0.02f * 2;
			float scale = 1f / render.resWidth * raySize;

			float p = 1 / 64f;

			GL11.glTranslatef(-raySize, 0, 0);
			GL11.glScalef(scale, -scale, 1);
			render.draw(0.4f,1.3f,0.8f);
			

		}

		GL11.glPopMatrix();
		UtilsClient.enableCulling();

		descriptor.draw(false, buttonsState, ledsAState, ledsPState);

	}

	boolean powerOk;

	float recalcTimeout = 0;
	
	public void refresh(float deltaT) {
		for (int idx = 0; idx < this.descriptor.ledsACount; idx++) {
			if (powerOk) {
				if (Math.random() < 0.2 * deltaT)
					ledsAState[idx] = !ledsAState[idx];
			} else {
				ledsAState[idx] = true;
			}
		}

		for (int idx = 0; idx < this.descriptor.ledsPCount; idx++) {
			if (powerOk) {
				if (Math.random() < 0.2 * deltaT)
					ledsPState[idx] = !ledsPState[idx];
			} else {
				ledsPState[idx] = true;
			}
		}

		if(powerOk){
			recalcTimeout -= deltaT;
			if(recalcTimeout < 0){
				recalcTimeout += 0.5;
				float camAlpha;
				switch (front) {
				default:
				case XN: camAlpha = (float)(Math.PI); break;
				case XP: camAlpha = 0; break;
				case ZN: camAlpha = (float)(-Math.PI/2); break;
				case ZP: camAlpha = (float)(Math.PI/2); break;

				}
				render.generate(this.tileEntity.getWorldObj(), tileEntity.xCoord + 0.5, tileEntity.yCoord + 0.5-(Math.max(0, pipeLength-5)), tileEntity.zCoord + 0.5, -(float) (Math.PI * 1 / 2) + camAlpha, -(float) (Math.PI / 2));
			}
		}
	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(AutoMinerContainer.inventorySize, 64, this);

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new AutoMinerGuiDraw(player, inventory, this);
	}

	short pipeLength = 0;
	AutoMinerSlowProcess.jobType job;

	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			if(pipeLength != (pipeLength = stream.readShort())){
				recalcTimeout = 0;
			}
			if(job != (job = AutoMinerSlowProcess.jobType.values()[stream.readByte()])){
			//	pushLog(job.toString());
			}
			powerOk = stream.readBoolean();

			
			
			if(!powerOk){
				logs.clear();
				pushLog("BOOT");
				pushLog("WAITING OPCODE");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void serverPacketUnserialize(DataInputStream stream) {
		try {
			switch (stream.readByte()) {
			case AutoMinerElement.pushLogId:
				pushLog(stream.readUTF());
				break;

			default:
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	
	@Override
	public boolean cameraDrawOptimisation() {
		return false;
	}
}
