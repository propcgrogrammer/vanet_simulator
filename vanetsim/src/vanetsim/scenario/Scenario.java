package vanetsim.scenario;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.ArrayDeque;

//import java16.util.ArrayDeque;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.in.SMInputCursor;
import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMOutputElement;

import vanetsim.ErrorLog;
import vanetsim.VanetSimStart;
import vanetsim.gui.Renderer;
import vanetsim.gui.helpers.AttackLogWriter;
import vanetsim.gui.helpers.MouseClickManager;
import vanetsim.gui.helpers.PrivacyLogWriter;
import vanetsim.localization.Messages;
import vanetsim.map.Map;
import vanetsim.map.Node;
import vanetsim.map.Region;
import vanetsim.routing.WayPoint;
import vanetsim.scenario.events.Event;
import vanetsim.scenario.events.EventList;
import vanetsim.scenario.events.StartBlocking;
import vanetsim.scenario.events.StopBlocking;

/**
 * A scenario saves the vehicles and events.
 */
public final class Scenario{

	/** The only instance of this class (singleton). */
	private static final Scenario INSTANCE = new Scenario();

	/** A flag to signal if loading is ready. While loading is in progress, simulation and rendering is not possible. */
	private boolean ready_ = true;

	/** File name of the Scenario. Used to name log files */
	private String scenarioName = "";
	
	/**
	 * Empty, private constructor in order to disable instancing.
	 */
	private Scenario() {
	}	

	/**
	 * Gets the single instance of this scenario.
	 * 
	 * @return single instance of this scenario
	 */
	public static Scenario getInstance(){
		return INSTANCE;
	}

	/**
	 * Initializes a new (empty) scenario.
	 */
	public void initNewScenario(){
		if(ready_ == true){
			ready_ = false;
			if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getSimulationMaster().stopThread();
			if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getSimulatePanel().setSimulationStop();
			KnownVehiclesList.setTimePassed(0);
			KnownRSUsList.setTimePassed(0);
			Renderer.getInstance().setTimePassed(0);
			Renderer.getInstance().setMarkedVehicle(null);
			Renderer.getInstance().setShowVehicles(false);
			Renderer.getInstance().setShowRSUs(false);
			Renderer.getInstance().setShowMixZones(false);
			Renderer.getInstance().setAttackedVehicle(null);
			Renderer.getInstance().setAttackerVehicle(null);
			Renderer.getInstance().setShowAttackers(false);
			Vehicle.setMaximumCommunicationDistance(0);
			Vehicle.resetGlobalRandomGenerator();
			Vehicle.setMinTravelTimeForRecycling(60000);	// standard value for recycle time
			Vehicle.setArsuList(new AttackRSU[0]);
			Vehicle.setAttackedVehicleID_(0);
			if(!Renderer.getInstance().isConsoleStart())MouseClickManager.getInstance().cleanMarkings();
			Region[][] Regions = Map.getInstance().getRegions();
			int Region_max_x = Map.getInstance().getRegionCountX();
			int Region_max_y = Map.getInstance().getRegionCountY();
			int i, j;
			for(i = 0; i < Region_max_x; ++i){
				for(j = 0; j < Region_max_y; ++j){
					Regions[i][j].cleanVehicles();
				}
			}
			EventList.getInstance().clearEvents();
			if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditEventPanel().updateList();
		}		
	}

	/**
	 * Load a scenario.
	 * 
	 * @param file	the file to load
	 * @param zip	<code>true</code> if the file given is zipped, else <code>false</code>
	 */
	public void load(File file, boolean zip){
		scenarioName = file.getName();
		Map.getInstance().clearMixZones();
		Map.getInstance().clearRSUs();
		try{
			if(!Renderer.getInstance().isConsoleStart())VanetSimStart.setProgressBar(true);
			initNewScenario();
			String type;
			int x, y, time, maxSpeed, vehicleLength, maxCommDistance, direction, lanes, braking_rate, acceleration_rate, timeDistance, politeness, color, mixX, mixY, mixRadius, wifiX, wifiY, wifiRadius;
			boolean tmpBoolean, wifi, emergencyVehicle, tmpAttacker, tmpAttacked, isEncrypted, mixHasRSU;
			ArrayDeque<WayPoint> destinations;
			WayPoint tmpWayPoint;
			Vehicle tmpVehicle;
			Node[] tmpNodes;
			Node tmpNode;
			SMInputCursor childCrsr, vehicleCrsr, vehiclesCrsr, mixNodeCrsr, mixNodesCrsr, settingsCrsr, eventCrsr, eventsCrsr, destinationsCrsr, waypointCrsr, rsuCrsr, rsusCrsr, aRsuCrsr, aRsusCrsr;
			XMLInputFactory factory = XMLInputFactory.newInstance();

			ErrorLog.log(Messages.getString("Scenario.loadingScenario") + file.getName(), 3, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
			// configure some factory options...
			factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
			factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
			factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
			
			Vehicle.setEncryptedBeaconsInMix_(false);

			InputStream filestream;
			if(zip){
				filestream = new ZipInputStream(new FileInputStream(file));
				((ZipInputStream) filestream).getNextEntry();
			} else filestream = new FileInputStream(file);
			XMLStreamReader sr = factory.createXMLStreamReader(filestream);	
			SMInputCursor rootCrsr = SMInputFactory.rootElementCursor(sr);
			rootCrsr.getNext();
			if(rootCrsr.getLocalName().toLowerCase().equals("scenario")){ //$NON-NLS-1$
				childCrsr = rootCrsr.childElementCursor();
				while(childCrsr.getNext() != null){
					if(childCrsr.getLocalName().toLowerCase().equals("settings")){	//$NON-NLS-1$
						settingsCrsr = childCrsr.childElementCursor();
						while (settingsCrsr.getNext() != null){
							if(settingsCrsr.getLocalName().toLowerCase().equals("communicationenabled")){ //$NON-NLS-1$
								if(settingsCrsr.collectDescendantText(false).equals("true")) tmpBoolean = true;	//$NON-NLS-1$
								else tmpBoolean = false;
								if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setCommunication(tmpBoolean);
								Vehicle.setCommunicationEnabled(tmpBoolean);
								RSU.setCommunicationEnabled(tmpBoolean);
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("beaconsenabled")){ //$NON-NLS-1$
								if(settingsCrsr.collectDescendantText(false).equals("true")) tmpBoolean = true;	//$NON-NLS-1$
								else tmpBoolean = false;
								if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setBeacons(tmpBoolean);
								Vehicle.setBeaconsEnabled(tmpBoolean);
								RSU.setBeaconsEnabled(tmpBoolean);
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("fallbackinmixzonesenabled")){ //$NON-NLS-1$
								if(settingsCrsr.collectDescendantText(false).equals("true")) tmpBoolean = true;	//$NON-NLS-1$
								else tmpBoolean = false;
								if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setMixZonesFallbackEnabled(tmpBoolean);
								Vehicle.setMixZonesFallbackEnabled(tmpBoolean);
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("fallbackinmixzonesfloodingonly")){ //$NON-NLS-1$
								if(settingsCrsr.collectDescendantText(false).equals("true")) tmpBoolean = true;	//$NON-NLS-1$
								else tmpBoolean = false;
								if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setMixZonesFallbackFloodingOnly(tmpBoolean);
								Vehicle.setMixZonesFallbackFloodingOnly(tmpBoolean);								
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("globalInfrastructureenabled")){ //$NON-NLS-1$
								if(settingsCrsr.collectDescendantText(false).equals("true")) tmpBoolean = true;	//$NON-NLS-1$
								else tmpBoolean = false;
								if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setGlobalInfrastructure(tmpBoolean);
								// implementation missing
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("mixzonesenabled")){ //$NON-NLS-1$
								if(settingsCrsr.collectDescendantText(false).equals("true")) tmpBoolean = true;	//$NON-NLS-1$
								else tmpBoolean = false;
								if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setMixZonesEnabled(tmpBoolean);
								Vehicle.setMixZonesEnabled(tmpBoolean);
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("vehiclerecyclingenabled")){ //$NON-NLS-1$
								if(settingsCrsr.collectDescendantText(false).equals("true")) tmpBoolean = true;	//$NON-NLS-1$
								else tmpBoolean = false;
								if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setRecyclingEnabled(tmpBoolean);
								Vehicle.setRecyclingEnabled(tmpBoolean);
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("communicationinterval")){ //$NON-NLS-1$
								try{
									int tmp = Integer.parseInt(settingsCrsr.collectDescendantText(false));
									if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setCommunicationInterval(tmp);
									Vehicle.setCommunicationInterval(tmp);
									RSU.setCommunicationInterval(tmp);
								} catch (Exception e) {}
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("beaconsinterval")){ //$NON-NLS-1$
								try{
									int tmp = Integer.parseInt(settingsCrsr.collectDescendantText(false));
									if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setBeaconInterval(tmp);
									Vehicle.setBeaconInterval(tmp);
									RSU.setBeaconInterval(tmp);
								} catch (Exception e) {}
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("mixzoneradius")){ //$NON-NLS-1$
								try{
									int tmp = Integer.parseInt(settingsCrsr.collectDescendantText(false));
									if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setMixZoneRadius(tmp);
									Vehicle.setMixZoneRadius(tmp);
								} catch (Exception e) {}
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("routingmode")){ //$NON-NLS-1$
								try{
									int tmp = Integer.parseInt(settingsCrsr.collectDescendantText(false));
									if(tmp > 1) tmp = 1;
									else if (tmp < 0) tmp = 0;
									if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditSettingsPanel().setRoutingMode(tmp);
									Vehicle.setRoutingMode(tmp);
								} catch (Exception e) {}
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("mintraveltimeforrecycling")){ //$NON-NLS-1$
								try{
									int tmp = Integer.parseInt(settingsCrsr.collectDescendantText(false));
									Vehicle.setMinTravelTimeForRecycling(tmp);
								} catch (Exception e) {}
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("arsulog")){ //$NON-NLS-1$
								try{
									String tmp = settingsCrsr.collectDescendantText(false);
									if(Vehicle.isAttackerDataLogged_()) AttackLogWriter.setLogPath(tmp);
								} catch (Exception e) {}	
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("arsuloggingenabled")){ //$NON-NLS-1$
								try{
									String tmp = settingsCrsr.collectDescendantText(false);
									if(tmp.equals("true")){
										if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditLogControlPanel_().getLogAttackerCheckBox_().setSelected(true);
										Vehicle.setAttackerDataLogged_(true);
									}
									else{
										if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditLogControlPanel_().getLogAttackerCheckBox_().setSelected(false);
										Vehicle.setAttackerDataLogged_(false);
									}
								} catch (Exception e) {}
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("arsuencryptedloggingenabled")){ //$NON-NLS-1$
								try{
									String tmp = settingsCrsr.collectDescendantText(false);
									if(tmp.equals("true")){
										if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditLogControlPanel_().getEncryptedLogging_().setSelected(true);
										Vehicle.setAttackerEncryptedDataLogged_(true);
									}
									else{
										if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditLogControlPanel_().getEncryptedLogging_().setSelected(false);
										Vehicle.setAttackerEncryptedDataLogged_(false);
									}
								} catch (Exception e) {}
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("privacyloggingenabled")){ //$NON-NLS-1$
								try{
									String tmp = settingsCrsr.collectDescendantText(false);
									if(tmp.equals("true")){
										if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditLogControlPanel_().getLogPrivacyCheckBox_().setSelected(true);
										Vehicle.setPrivacyDataLogged_(true);
										PrivacyLogWriter.setLogPath(System.getProperty("user.dir"));
										System.out.println("logged:" + Vehicle.isPrivacyDataLogged_() + " pfad: " + PrivacyLogWriter.getLogPath());
									}
									else{
										if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditLogControlPanel_().getLogPrivacyCheckBox_().setSelected(false);
										Vehicle.setPrivacyDataLogged_(false);
									}
								} catch (Exception e) {}	
								
								
								
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("showencryptedcomminmix")){ //$NON-NLS-1$
								try{
									String tmp = settingsCrsr.collectDescendantText(false);
									if(tmp.equals("true")){
										if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditMixZonePanel_().getShowEncryptedBeacons_().setSelected(true);
										RSU.setShowEncryptedBeaconsInMix_(true);
									}
									else{
										if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditMixZonePanel_().getShowEncryptedBeacons_().setSelected(false);
										RSU.setShowEncryptedBeaconsInMix_(false);
									}
								} catch (Exception e) {}	
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("silentperiodsenabled")){ //$NON-NLS-1$
								try{
									String tmp = settingsCrsr.collectDescendantText(false);
									if(tmp.equals("true")){
										Vehicle.setSilentPeriodsOn(true);
									}
									else{
										Vehicle.setSilentPeriodsOn(false);
									}
								} catch (Exception e) {}	
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("silentperiodduration")){ //$NON-NLS-1$
								try{
									int tmp = Integer.parseInt(settingsCrsr.collectDescendantText(false));
									Vehicle.setTIME_OF_SILENT_PERIODS(tmp);
								} catch (Exception e) {}
							} else if(settingsCrsr.getLocalName().toLowerCase().equals("silentperiodfrequency")){ //$NON-NLS-1$
								try{
									int tmp = Integer.parseInt(settingsCrsr.collectDescendantText(false));
									Vehicle.setTIME_BETWEEN_SILENT_PERIODS(tmp);
								} catch (Exception e) {}
							}
						}
					} else if(childCrsr.getLocalName().toLowerCase().equals("vehicles")){	//$NON-NLS-1$
						vehiclesCrsr = childCrsr.childElementCursor();
						while (vehiclesCrsr.getNext() != null){
							if(vehiclesCrsr.getLocalName().toLowerCase().equals("vehicle")){ //$NON-NLS-1$
								maxCommDistance = 10000;
								vehicleLength = 2500;
								maxSpeed = 10000;
								wifi = true;
								emergencyVehicle = false;
								braking_rate = 100;
								acceleration_rate = 200;
								timeDistance = 100;
								politeness = 50;
								color = 0;
								destinations = new ArrayDeque<WayPoint>(1);
								vehicleCrsr = vehiclesCrsr.childElementCursor();
								tmpAttacker = false;
								tmpAttacked = false;
								while (vehicleCrsr.getNext() != null){
									if(vehicleCrsr.getLocalName().toLowerCase().equals("vehiclelength")){ //$NON-NLS-1$
										try{
											vehicleLength = Integer.parseInt(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("maxspeed")){ //$NON-NLS-1$
										try{
											maxSpeed = Integer.parseInt(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("maxcommdist")){ //$NON-NLS-1$
										try{
											maxCommDistance = Integer.parseInt(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("wifi")){ //$NON-NLS-1$
										try{
											wifi = Boolean.parseBoolean(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("emergencyvehicle")){ //$NON-NLS-1$
										try{
											emergencyVehicle = Boolean.parseBoolean(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("braking_rate")){ //$NON-NLS-1$
										try{
											braking_rate = Integer.parseInt(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("acceleration_rate")){ //$NON-NLS-1$
										try{
											acceleration_rate = Integer.parseInt(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("timedistance")){ //$NON-NLS-1$
										try{
											timeDistance = Integer.parseInt(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("politeness")){ //$NON-NLS-1$
										try{
											politeness = Integer.parseInt(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("color")){ //$NON-NLS-1$
										try{
											color = Integer.parseInt(vehicleCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("isattacker")){ //$NON-NLS-1$
										try{
											if(vehicleCrsr.collectDescendantText(false).equals("true")) tmpAttacker = true;
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("isattacked")){ //$NON-NLS-1$
										try{
											if(vehicleCrsr.collectDescendantText(false).equals("true")) tmpAttacked = true;
										} catch (Exception e) {}
									} else if(vehicleCrsr.getLocalName().toLowerCase().equals("destinations")){ //$NON-NLS-1$
										destinationsCrsr = vehicleCrsr.childElementCursor();
										while (destinationsCrsr.getNext() != null){
											if(destinationsCrsr.getLocalName().toLowerCase().equals("waypoint")){ //$NON-NLS-1$
												x = -1;
												y = -1;
												time = -1;
												waypointCrsr = destinationsCrsr.childElementCursor();
												while (waypointCrsr.getNext() != null){
													if(waypointCrsr.getLocalName().toLowerCase().equals("x")){ //$NON-NLS-1$
														try{
															x = Integer.parseInt(waypointCrsr.collectDescendantText(false));
														} catch (Exception e) {}
													} else if(waypointCrsr.getLocalName().toLowerCase().equals("y")){ //$NON-NLS-1$
														try{
															y = Integer.parseInt(waypointCrsr.collectDescendantText(false));
														} catch (Exception e) {}
													} else if(waypointCrsr.getLocalName().toLowerCase().equals("wait")){ //$NON-NLS-1$
														try{
															time = Integer.parseInt(waypointCrsr.collectDescendantText(false));
														} catch (Exception e) {}
													} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileWayPoint") + waypointCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
												}
												try{												
													tmpWayPoint = new WayPoint(x,y,time);
													destinations.add(tmpWayPoint);
												} catch (ParseException e) { 
													ErrorLog.log(Messages.getString("Scenario.snappingFailed"), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
												}											
											}
										}
									} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileVehicle") + vehicleCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
								}
								if(maxCommDistance != -1 && maxSpeed != -1 && destinations.size() > 1){
									try{
										tmpVehicle = new Vehicle(destinations, vehicleLength, maxSpeed, maxCommDistance, wifi, emergencyVehicle, braking_rate, acceleration_rate, timeDistance, politeness, new Color(color));
										Map.getInstance().addVehicle(tmpVehicle);
										if(tmpAttacker) Renderer.getInstance().setAttackerVehicle(tmpVehicle);
										if(tmpAttacked) {
											Renderer.getInstance().setAttackedVehicle(tmpVehicle);
											Vehicle.setAttackedVehicleID_(tmpVehicle.getID());
										}
										tmpAttacker = false;
										tmpAttacked = false;
									} catch (Exception e){}
								} else ErrorLog.log(Messages.getString("Scenario.notAllFieldsForVehicle"), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
							} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileVehicles") + vehiclesCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
						}
					} else if(childCrsr.getLocalName().toLowerCase().equals("mixzones")){	//$NON-NLS-1$
						mixNodesCrsr = childCrsr.childElementCursor();
						int maxMixRadius = 0;
						while (mixNodesCrsr.getNext() != null){
							if(mixNodesCrsr.getLocalName().toLowerCase().equals("mixnode")){ //$NON-NLS-1$
								mixX = -1;
								mixY = -1;
								mixRadius = -1;
								mixHasRSU = false;

								mixNodeCrsr = mixNodesCrsr.childElementCursor();
								while (mixNodeCrsr.getNext() != null){
									if(mixNodeCrsr.getLocalName().toLowerCase().equals("x")){ //$NON-NLS-1$
										try{
											mixX = Integer.parseInt(mixNodeCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(mixNodeCrsr.getLocalName().toLowerCase().equals("y")){ //$NON-NLS-1$
										try{
											mixY = Integer.parseInt(mixNodeCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(mixNodeCrsr.getLocalName().toLowerCase().equals("radius")){ //$NON-NLS-1$
										try{
											mixRadius = Integer.parseInt(mixNodeCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(mixNodeCrsr.getLocalName().toLowerCase().equals("hasrsu")){ //$NON-NLS-1$
										try{
											if(mixNodeCrsr.collectDescendantText(false).equals("true")) mixHasRSU = true;
										} catch (Exception e) {}
									} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileMixNode") + mixNodeCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
								}
								if(mixRadius > 0){
									try{
										int Region_cnt_x = Map.getInstance().getRegionCountX();
										int Region_cnt_y = Map.getInstance().getRegionCountY();
										Region[][] Regions = Map.getInstance().getRegions();
										for(int i = 0; i < Region_cnt_x; ++i){
											for(int j = 0; j < Region_cnt_y; ++j){
												tmpNodes = Regions[i][j].getNodes();		
												for(int k = 0; k < tmpNodes.length;k++){
													tmpNode = tmpNodes[k];
													if(tmpNode.getX() == mixX && tmpNode.getY() == mixY){
														if(mixHasRSU)Vehicle.setEncryptedBeaconsInMix_(true);
														Regions[i][j].addMixZone(tmpNode, mixRadius);
														if(maxMixRadius < mixRadius)maxMixRadius = mixRadius;
														Vehicle.setEncryptedBeaconsInMix_(false);
														if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditMixZonePanel_().getEncryptedBeacons_().setSelected(false);
													}
												}
											}
										}
									} catch (Exception e){}
								} else ErrorLog.log(Messages.getString("Scenario.notAllFieldsForMixNode"), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
							} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileMixNodes") + mixNodesCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
						}
						Vehicle.setMaxMixZoneRadius(maxMixRadius);
					} else if(childCrsr.getLocalName().toLowerCase().equals("rsus")){	//$NON-NLS-1$
						rsusCrsr = childCrsr.childElementCursor();
						while (rsusCrsr.getNext() != null){
							if(rsusCrsr.getLocalName().toLowerCase().equals("rsu")){ //$NON-NLS-1$
								wifiX = -1;
								wifiY = -1;
								wifiRadius = -1;
								isEncrypted = false;
								rsuCrsr = rsusCrsr.childElementCursor();
								while (rsuCrsr.getNext() != null){
									if(rsuCrsr.getLocalName().toLowerCase().equals("x")){ //$NON-NLS-1$
										try{
											wifiX = Integer.parseInt(rsuCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(rsuCrsr.getLocalName().toLowerCase().equals("y")){ //$NON-NLS-1$
										try{
											wifiY = Integer.parseInt(rsuCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(rsuCrsr.getLocalName().toLowerCase().equals("radius")){ //$NON-NLS-1$
										try{
											wifiRadius = Integer.parseInt(rsuCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(rsuCrsr.getLocalName().toLowerCase().equals("isencrypted")){ //$NON-NLS-1$
										try{
											if(rsuCrsr.collectDescendantText(false).equals("true")) isEncrypted = true;
										} catch (Exception e) {}
									} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileRSU") + rsuCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
								}
								try{
									if(!isEncrypted)Map.getInstance().addRSU(new RSU(wifiX,wifiY,wifiRadius,false));
								} catch (Exception e){}
							} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileRSUs") + rsusCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
						}	
					} else if(childCrsr.getLocalName().toLowerCase().equals("arsus")){	//$NON-NLS-1$
						aRsusCrsr = childCrsr.childElementCursor();
						while (aRsusCrsr.getNext() != null){
							if(aRsusCrsr.getLocalName().toLowerCase().equals("arsu")){ //$NON-NLS-1$
								wifiX = -1;
								wifiY = -1;
								wifiRadius = -1;

								aRsuCrsr = aRsusCrsr.childElementCursor();
								while (aRsuCrsr.getNext() != null){
									if(aRsuCrsr.getLocalName().toLowerCase().equals("x")){ //$NON-NLS-1$
										try{
											wifiX = Integer.parseInt(aRsuCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(aRsuCrsr.getLocalName().toLowerCase().equals("y")){ //$NON-NLS-1$
										try{
											wifiY = Integer.parseInt(aRsuCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(aRsuCrsr.getLocalName().toLowerCase().equals("radius")){ //$NON-NLS-1$
										try{
											wifiRadius = Integer.parseInt(aRsuCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileARSU") + aRsuCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
								}
								try{
									new AttackRSU(wifiX,wifiY,wifiRadius);
								} catch (Exception e){}
							} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileARSUs") + aRsusCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
						}	
					} else if(childCrsr.getLocalName().toLowerCase().equals("events")){ //$NON-NLS-1$
						eventsCrsr = childCrsr.childElementCursor();
						while (eventsCrsr.getNext() != null){
							if(eventsCrsr.getLocalName().toLowerCase().equals("event")){ //$NON-NLS-1$
								time = -1;
								x = -1;
								y = -1;
								direction = 0;
								lanes = Integer.MAX_VALUE;
								type = ""; //$NON-NLS-1$
								eventCrsr = eventsCrsr.childElementCursor();
								while (eventCrsr.getNext() != null){
									if(eventCrsr.getLocalName().toLowerCase().equals("time")){ //$NON-NLS-1$
										try{
											time = Integer.parseInt(eventCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(eventCrsr.getLocalName().toLowerCase().equals("type")){ //$NON-NLS-1$
										type = eventCrsr.collectDescendantText(false).toLowerCase();
									} else if(eventCrsr.getLocalName().toLowerCase().equals("x")){ //$NON-NLS-1$
										try{
											x = Integer.parseInt(eventCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(eventCrsr.getLocalName().toLowerCase().equals("y")){ //$NON-NLS-1$
										try{
											y = Integer.parseInt(eventCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(eventCrsr.getLocalName().toLowerCase().equals("direction")){ //$NON-NLS-1$
										try{
											direction = Integer.parseInt(eventCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else if(eventCrsr.getLocalName().toLowerCase().equals("lanes")){ //$NON-NLS-1$
										try{
											lanes = Integer.parseInt(eventCrsr.collectDescendantText(false));
										} catch (Exception e) {}
									} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileEvent") + childCrsr.getLocalName(), 5, getClass().getName(), "load", null); //$NON-NLS-1$ //$NON-NLS-2$
								}
								if(time != -1){
									if(type.equals("startblocking") && x != -1 && y != -1){ //$NON-NLS-1$
										try{
											EventList.getInstance().addEvent(new StartBlocking(time, x, y, direction, lanes));
										} catch (Exception e) {}
									} else if(type.equals("stopblocking") && x != -1 && y != -1){ //$NON-NLS-1$
										try{
											EventList.getInstance().addEvent(new StopBlocking(time, x, y));
										} catch (Exception e) {}
									}
								}
							} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileEvents") + childCrsr.getLocalName(), 5, getClass().getName(), "load", null); //$NON-NLS-1$ //$NON-NLS-2$
						}
					} else ErrorLog.log(Messages.getString("Scenario.unknownElementWhileScenario") + childCrsr.getLocalName(), 5, getClass().getName(), "load", null);  //$NON-NLS-1$//$NON-NLS-2$
				}
			} else ErrorLog.log(Messages.getString("Scenario.wrongRoot"), 7, getClass().getName(), "load", null); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {ErrorLog.log(Messages.getString("Scenario.errorLoading"), 7, getClass().getName(), "load", e);} //$NON-NLS-1$ //$NON-NLS-2$
		if(!Renderer.getInstance().isConsoleStart())VanetSimStart.setProgressBar(false);
		ready_ = true;
		Renderer.getInstance().ReRender(false, false);
		if(!Renderer.getInstance().isConsoleStart())VanetSimStart.getMainControlPanel().getEditPanel().getEditEventPanel().updateList();
		ErrorLog.log(Messages.getString("Scenario.finishedLoading"), 3, getClass().getName(), "load", null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Sets the ready state of the scenario.
	 * 
	 * @param ready	<code>true</code> to signal that this scenario is ready with loading, else <code>false</code>
	 */
	public void setReadyState(boolean ready){
		ready_ = ready;
	}

	/**
	 * Save the scenario.
	 * 
	 * @param file	the file in which to save
	 * @param zip	if <code>true</code>, file is saved in a compressed zip file (extension .zip is added to <code>file</code>!). If <code>false</code>, no compression is made.
	 */
	public void save(File file, boolean zip){
		try{
			VanetSimStart.setProgressBar(true);
			ErrorLog.log(Messages.getString("Scenario.savingScenario") + file.getName(), 3, getClass().getName(), "save", null);  //$NON-NLS-1$//$NON-NLS-2$
			int i, j, k;
			Vehicle[] vehiclesArray;
			Node[] mixZoneArray;
			ArrayDeque<WayPoint> destinations;
			Iterator<WayPoint> wayPointIterator;
			Vehicle vehicle;
			Node mixNode;
			WayPoint wayPoint;
			RSU rsu;
			RSU[] rsuArray;
			Event event;
			SMOutputElement level1, level2, level3;

			int Region_cnt_x = Map.getInstance().getRegionCountX();
			int Region_cnt_y = Map.getInstance().getRegionCountY();
			Region[][] Regions = Map.getInstance().getRegions();

			OutputStream filestream;
			if(zip){
				filestream = new ZipOutputStream(new FileOutputStream(file + ".zip")); //$NON-NLS-1$
				((ZipOutputStream) filestream).putNextEntry(new ZipEntry(file.getName()));
			} else filestream = new FileOutputStream(file);
			XMLStreamWriter xw = XMLOutputFactory.newInstance().createXMLStreamWriter(filestream);
			SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);
			doc.setIndentation("\n\t\t\t\t\t\t\t\t", 2, 1); ;  //$NON-NLS-1$
			doc.addComment("Generated on " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())); //$NON-NLS-1$ //$NON-NLS-2$

			SMOutputElement root = doc.addElement("Scenario");			 //$NON-NLS-1$
			SMOutputElement settings = root.addElement("Settings");			 //$NON-NLS-1$
			settings.addElement("CommunicationEnabled").addValue(Vehicle.getCommunicationEnabled()); //$NON-NLS-1$
			settings.addElement("BeaconsEnabled").addValue(Vehicle.getBeaconsEnabled()); //$NON-NLS-1$
			settings.addElement("GlobalInfrastructureEnabled").addValue(true);	//$NON-NLS-1$
			settings.addElement("CommunicationInterval").addValue(Vehicle.getCommunicationInterval()); //$NON-NLS-1$
			settings.addElement("BeaconsInterval").addValue(Vehicle.getBeaconInterval()); //$NON-NLS-1$
			settings.addElement("MixZonesEnabled").addValue(Vehicle.getMixZonesEnabled()); //$NON-NLS-1$
			settings.addElement("MixZoneRadius").addValue(Vehicle.getMixZoneRadius()); //$NON-NLS-1$
			settings.addElement("RoutingMode").addValue(Vehicle.getRoutingMode()); //$NON-NLS-1$
			settings.addElement("VehicleRecyclingEnabled").addValue(Vehicle.getRecyclingEnabled()); //$NON-NLS-1$
			settings.addElement("FallBackInMixZonesEnabled").addValue(Vehicle.getMixZonesFallbackEnabled());	//$NON-NLS-1$
			settings.addElement("FallBackInMixZonesFloodingOnly").addValue(Vehicle.getMixZonesFallbackFloodingOnly());	//$NON-NLS-1$
			settings.addElement("MinTravelTimeForRecycling").addValue(Vehicle.getMinTravelTimeForRecycling());	//$NON-NLS-1$
			
			//arsu settings
			settings.addElement("ARSULoggingEnabled").addValue(Vehicle.isAttackerDataLogged_());
			settings.addElement("ARSULog").addCharacters(AttackLogWriter.getLogPath());	//$NON-NLS-1$
			settings.addElement("ARSUEncryptedLoggingEnabled").addValue(Vehicle.isAttackerEncryptedDataLogged_());
			
			//privacy log
			settings.addElement("privacyLoggingEnabled").addValue(Vehicle.isPrivacyDataLogged_());
			
			//silent period settings
			settings.addElement("SilentPeriodsEnabled").addValue(Vehicle.isSilentPeriodsOn());
			settings.addElement("SilentPeriodDuration").addValue(Vehicle.getTIME_OF_SILENT_PERIODS()); //$NON-NLS-1$
			settings.addElement("SilentPeriodFrequency").addValue(Vehicle.getTIME_BETWEEN_SILENT_PERIODS()); //$NON-NLS-1$

			if(RSU.isShowEncryptedBeaconsInMix_())settings.addElement("showEncryptedCommInMix").addCharacters("true");
			else settings.addElement("showEncryptedCommInMix").addCharacters("false");
			SMOutputElement vehicles = root.addElement("Vehicles");			 //$NON-NLS-1$			
		
			for(i = 0; i < Region_cnt_x; ++i){
				for(j = 0; j < Region_cnt_y; ++j){
					vehiclesArray = Regions[i][j].getVehicleArray();
					for(k = 0; k < vehiclesArray.length; ++k){
						vehicle = vehiclesArray[k];
						level1 = vehicles.addElement("Vehicle"); //$NON-NLS-1$
						level1.addElement("VehicleLength").addValue(vehicle.getVehicleLength()); //$NON-NLS-1$
						level1.addElement("MaxSpeed").addValue(vehicle.getMaxSpeed()); //$NON-NLS-1$
						level1.addElement("MaxCommDist").addValue(vehicle.getMaxCommDistance()); //$NON-NLS-1$
						level1.addElement("Wifi").addValue(vehicle.isWiFiEnabled()); //$NON-NLS-1$
						level1.addElement("emergencyVehicle").addValue(vehicle.isEmergencyVehicle()); //$NON-NLS-1$
						level1.addElement("braking_rate").addValue(vehicle.getBrakingRate()); //$NON-NLS-1$
						level1.addElement("acceleration_rate").addValue(vehicle.getAccelerationRate()); //$NON-NLS-1$
						level1.addElement("timeDistance").addValue(vehicle.getTimeDistance()); //$NON-NLS-1$
						level1.addElement("politeness").addValue(vehicle.getPoliteness()); //$NON-NLS-1$
						level1.addElement("Color").addValue(vehicle.getColor().getRGB()); //$NON-NLS-1$
						if(Renderer.getInstance().getAttackerVehicle() == vehicle) level1.addElement("isAttacker").addValue(true); //$NON-NLS-1$
						else level1.addElement("isAttacker").addValue(false);
						if(Renderer.getInstance().getAttackedVehicle() == vehicle) level1.addElement("isAttacked").addValue(true); //$NON-NLS-1$
						else level1.addElement("isAttacked").addValue(false);
						level2 = level1.addElement("Destinations"); //$NON-NLS-1$
						//add the start point manually as this was already "popped" from the vehicle's destinations
						level3 = level2.addElement("WayPoint");	//$NON-NLS-1$
						level3.addElement("x").addValue(vehicle.getX());	//$NON-NLS-1$
						level3.addElement("y").addValue(vehicle.getY());	//$NON-NLS-1$
						level3.addElement("wait").addValue(vehicle.getWaittime());	//$NON-NLS-1$
						destinations = vehicle.getDestinations();
						wayPointIterator = destinations.iterator();	//iterate through destinations
						while(wayPointIterator.hasNext()){
							wayPoint = wayPointIterator.next();
							level3 = level2.addElement("WayPoint");	//$NON-NLS-1$
							level3.addElement("x").addValue(wayPoint.getX());	//$NON-NLS-1$
							level3.addElement("y").addValue(wayPoint.getY());	//$NON-NLS-1$
							level3.addElement("wait").addValue(wayPoint.getWaittime());	//$NON-NLS-1$
						}

					}
				}
			}
			
			//save mix zones
			SMOutputElement mixZones = root.addElement("MixZones");			 //$NON-NLS-1$			

			for(i = 0; i < Region_cnt_x; ++i){
				for(j = 0; j < Region_cnt_y; ++j){
					mixZoneArray = Regions[i][j].getMixZoneNodes();
					for(k = 0; k < mixZoneArray.length; ++k){
						mixNode = mixZoneArray[k];
						level1 = mixZones.addElement("MixNode"); //$NON-NLS-1$
						level1.addElement("x").addValue(mixNode.getX()); //$NON-NLS-1$
						level1.addElement("y").addValue(mixNode.getY()); //$NON-NLS-1$
						level1.addElement("radius").addValue(mixNode.getMixZoneRadius()); //$NON-NLS-1$
						if(mixNode.getEncryptedRSU_() != null) level1.addElement("hasRSU").addValue(true); //$NON-NLS-1$
						else level1.addElement("hasRSU").addValue(false); //$NON-NLS-1$

					}
				}
			}
			
			//save rsus
			SMOutputElement rsus = root.addElement("RSUs");			 //$NON-NLS-1$			

			for(i = 0; i < Region_cnt_x; ++i){
				for(j = 0; j < Region_cnt_y; ++j){
					rsuArray = Regions[i][j].getRSUs();
					for(k = 0; k < rsuArray.length; ++k){
						rsu = rsuArray[k];
						if(!rsu.isEncrypted_()){
							level1 = rsus.addElement("RSU"); //$NON-NLS-1$
							level1.addElement("x").addValue(rsu.getX()); //$NON-NLS-1$
							level1.addElement("y").addValue(rsu.getY()); //$NON-NLS-1$
							level1.addElement("radius").addValue(rsu.getWifiRadius()); //$NON-NLS-1$
							level1.addElement("isEncrypted").addValue(rsu.isEncrypted_()); //$NON-NLS-1$
						}
					}
				}
			}
			
			//save arsus
			SMOutputElement arsus = root.addElement("ARSUs");			 //$NON-NLS-1$			

			AttackRSU[] tempARSUList = Vehicle.getArsuList();
			
		    for(int l = 0; l < tempARSUList.length;l++) {
				level1 = arsus.addElement("ARSU"); //$NON-NLS-1$
				level1.addElement("x").addValue(tempARSUList[l].getX()); //$NON-NLS-1$
				level1.addElement("y").addValue(tempARSUList[l].getY()); //$NON-NLS-1$
				level1.addElement("radius").addValue(tempARSUList[l].getWifiRadius()); //$NON-NLS-1$	
		      }

			
			SMOutputElement events = root.addElement("Events");			 //$NON-NLS-1$			
			Iterator<Event> eventIterator = EventList.getInstance().getIterator();
			while(eventIterator.hasNext()){
				event = eventIterator.next();
				level1 = events.addElement("Event"); //$NON-NLS-1$
				level1.addElement("Time").addValue(event.getTime()); //$NON-NLS-1$
				if(event.getClass() == StartBlocking.class){
					level1.addElement("Type").addCharacters("startBlocking"); //$NON-NLS-1$ //$NON-NLS-2$
					level1.addElement("x").addValue(((StartBlocking)event).getX()); //$NON-NLS-1$
					level1.addElement("y").addValue(((StartBlocking)event).getY()); //$NON-NLS-1$
					level1.addElement("Direction").addValue(((StartBlocking)event).getAffectedDirection()); //$NON-NLS-1$
					level1.addElement("Lanes").addValue(((StartBlocking)event).getAffectedLanes()); //$NON-NLS-1$
				} else if(event.getClass() == StopBlocking.class) {
					level1.addElement("Type").addCharacters("stopBlocking"); //$NON-NLS-1$ //$NON-NLS-2$
					level1.addElement("x").addValue(((StopBlocking)event).getX()); //$NON-NLS-1$
					level1.addElement("y").addValue(((StopBlocking)event).getY()); //$NON-NLS-1$
				} else ErrorLog.log(Messages.getString("Scenario.unknownEvent"), 6, getClass().getName(), "save", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			doc.closeRoot();
			xw.close();
			filestream.close();
		}catch (Exception e) { ErrorLog.log(Messages.getString("Scenario.errorWhileSaving"), 6, getClass().getName(), "save", e);} //$NON-NLS-1$ //$NON-NLS-2$
		VanetSimStart.setProgressBar(false);
	}

	/**
	 * Returns if the scenario is currently being loaded. While loading, simulation and rendering should not 
	 * be done because not all simulation elements are already existing!
	 * 
	 * @return <code>true</code> if loading has finished, else <code>false</code>
	 */
	public boolean getReadyState(){
		return ready_;
	}

	public String getScenarioName() {
		return scenarioName;
	}

	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}
}