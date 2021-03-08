/*
 *
 * Created on 12. September 2006, 09:57
 *
 * Copyright (c) 2006 LinogistiX GmbH. All rights reserved.
 *
 *<a href="http://www.linogistix.com/">browse for licence information</a>
 *
 */
package de.linogistix.los.example;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.mywms.facade.FacadeException;
import org.mywms.model.BasicEntity;
import org.mywms.model.Client;
import org.mywms.service.ClientService;
import org.mywms.service.RoleService;
import org.mywms.service.UserService;

import de.linogistix.los.crud.BusinessObjectCreationException;
import de.linogistix.los.crud.BusinessObjectExistsException;
import de.linogistix.los.crud.BusinessObjectMergeException;
import de.linogistix.los.crud.BusinessObjectModifiedException;
import de.linogistix.los.crud.ClientCRUDRemote;
import de.linogistix.los.inventory.service.ItemUnitService;
import de.linogistix.los.location.crud.LOSAreaCRUDRemote;
import de.linogistix.los.location.crud.LOSStorageLocationCRUDRemote;
import de.linogistix.los.location.crud.LOSStorageLocationTypeCRUDRemote;
import de.linogistix.los.location.crud.LOSTypeCapacityConstraintCRUDRemote;
import de.linogistix.los.location.crud.UnitLoadCRUDRemote;
import de.linogistix.los.location.crud.UnitLoadTypeCRUDRemote;
import de.linogistix.los.location.entityservice.LOSStorageLocationService;
import de.linogistix.los.location.entityservice.LOSStorageLocationTypeService;
import de.linogistix.los.location.query.LOSAreaQueryRemote;
import de.linogistix.los.location.query.LOSFixedLocationAssignmentQueryRemote;
import de.linogistix.los.location.query.LOSStorageLocationQueryRemote;
import de.linogistix.los.location.query.LOSStorageLocationTypeQueryRemote;
import de.linogistix.los.location.query.LOSTypeCapacityConstraintQueryRemote;
import de.linogistix.los.location.query.UnitLoadQueryRemote;
import de.linogistix.los.location.query.UnitLoadTypeQueryRemote;
import de.linogistix.los.location.service.QueryTypeCapacityConstraintService;
import de.linogistix.los.query.ClientQueryRemote;
import de.linogistix.los.query.QueryDetail;
import de.linogistix.los.query.TemplateQuery;
import de.linogistix.los.query.TemplateQueryWhereToken;
import de.linogistix.los.query.exception.BusinessObjectNotFoundException;
import de.linogistix.los.runtime.BusinessObjectSecurityException;
import de.wms2.mywms.inventory.StockUnit;
import de.wms2.mywms.inventory.UnitLoad;
import de.wms2.mywms.inventory.UnitLoadType;
import de.wms2.mywms.location.Area;
import de.wms2.mywms.location.AreaUsages;
import de.wms2.mywms.location.LocationType;
import de.wms2.mywms.location.LocationTypeEntityService;
import de.wms2.mywms.location.StorageLocation;
import de.wms2.mywms.strategy.FixAssignment;
import de.wms2.mywms.strategy.TypeCapacityConstraint;

/**
 * Creates an example topology
 * 
 * @author <a href="http://community.mywms.de/developer.jsp">Andreas Trautmann</a>
 */
@Stateless()
public class LocationTestTopologyBean implements LocationTestTopologyRemote {

	private static final Logger log = Logger.getLogger(LocationTestTopologyBean.class);
	
	// --------------------------------------------------------------------------
	Client SYSTEMCLIENT;
	Client TESTCLIENT;
	Client TESTMANDANT;
		
	protected UnitLoadType KLT;
		
	protected LocationType PALETTENPLATZ_TYP_2;
		
	protected LocationType KOMMPLATZ_TYP;
	
	protected TypeCapacityConstraint VIELE_PALETTEN;
	
	protected TypeCapacityConstraint EINE_PALETTE;
	
	protected TypeCapacityConstraint KOMM_FACH_DUMMY_LHM_CONSTR;
	
	protected Area STORE_AREA;
	
	protected Area KOMM_AREA;
	
	protected Area WE_BEREICH;
	
	protected Area WA_BEREICH;
	
	protected Area CLEARING_BEREICH;
	
	protected Area PRODUCTION_BEREICH;
	
	protected StorageLocation SL_WE;
	
	protected StorageLocation SL_PRODUCTION;
	
	protected String TEST_RACK_1;
	
	protected String TEST_RACK_2;
	
	protected StorageLocation SL_WA;
	
	protected StorageLocation SL_NIRWANA;
	
	protected StorageLocation SL_CLEARING;
		
	
	@EJB
    RoleService roleService;
    @EJB
    UserService userService;
    @EJB
    ClientService clientService;
	@EJB
	ClientQueryRemote clientQuery;
	@EJB
	ClientCRUDRemote clientCrud;
	
	@EJB
	UnitLoadTypeQueryRemote ulTypeQuery;
	@EJB
	UnitLoadTypeCRUDRemote ulTypeCrud;
	@EJB
	LOSStorageLocationTypeQueryRemote slTypeQuery;
	@EJB
	LOSStorageLocationTypeService slTypeService;
	@EJB
	LOSStorageLocationTypeCRUDRemote slTypeCrud;
	@EJB
	LOSTypeCapacityConstraintQueryRemote typeCapacityConstaintQuery;
	@EJB
	LOSTypeCapacityConstraintCRUDRemote typeCapacityConstaintCrud;
	@EJB
	QueryTypeCapacityConstraintService typeCapacityConstaintService;
	@EJB
	LOSAreaQueryRemote areaQuery;
	@EJB
	LOSAreaCRUDRemote areaCrud;
	@EJB
	LOSStorageLocationQueryRemote slQuery;
	@EJB
	LOSStorageLocationCRUDRemote slCrud;
	@EJB
	UnitLoadQueryRemote ulQuery;
	@EJB
	UnitLoadCRUDRemote ulCrud;
	
	@EJB
	LOSFixedLocationAssignmentQueryRemote assQuery;
	
	@EJB
	ItemUnitService itemUnitService;
    @EJB
    LOSStorageLocationService locationService;
	@PersistenceContext(unitName = "myWMS")
	protected EntityManager em;

	@Inject
	LocationTypeEntityService slTypeEntityService;

	/** Creates a new instance of TopologyBean */
	public LocationTestTopologyBean() {
	}
	
	//---------------------------------------------------
	
	//-----------------------------------------------------------------

	public void create() throws LocationTopologyException {
		try {
			createClients();
			
			createUnitLoadTypes();
			createStorageLocationsTyp();
			createCapacityConstraints();
			createAreas();
			
			createStorageLocations();
			createRacks();
			em.flush();
		} catch (FacadeException ex) {
			log.error(ex, ex);
			throw new LocationTopologyException();
		}

	}

	public void createClients() throws LocationTopologyException,
			BusinessObjectExistsException, BusinessObjectCreationException,
			BusinessObjectSecurityException {

		SYSTEMCLIENT = clientQuery.getSystemClient();

		if (SYSTEMCLIENT == null) {
			log.error("No System CLient found");
			throw new LocationTopologyException();
		}

		try {
			TESTCLIENT = clientQuery.queryByIdentity(CommonTestTopologyRemote.TESTCLIENT_NUMBER);
		} catch (BusinessObjectNotFoundException ex) {
			TESTCLIENT = new Client();
			TESTCLIENT.setName(CommonTestTopologyRemote.TESTCLIENT_NUMBER);
			TESTCLIENT.setNumber(CommonTestTopologyRemote.TESTCLIENT_NUMBER);
			TESTCLIENT.setCode(CommonTestTopologyRemote.TESTCLIENT_NUMBER);
			em.persist(TESTCLIENT);
		}
		try {
			TESTMANDANT = clientQuery.queryByIdentity(CommonTestTopologyRemote.TESTMANDANT_NUMBER);
		} catch (BusinessObjectNotFoundException ex) {
			TESTMANDANT = new Client();
			TESTMANDANT.setName(CommonTestTopologyRemote.TESTMANDANT_NUMBER);
			TESTMANDANT.setNumber(CommonTestTopologyRemote.TESTMANDANT_NUMBER);
			TESTMANDANT.setCode(CommonTestTopologyRemote.TESTMANDANT_NUMBER);
			em.persist(TESTMANDANT);
		}
		
	}

	public void createUnitLoadTypes() throws LocationTopologyException,
			BusinessObjectExistsException, BusinessObjectCreationException,
			BusinessObjectSecurityException {		
		
		try {
			KLT = ulTypeQuery.queryByIdentity(KLT_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			KLT = new UnitLoadType();
			KLT.setName(KLT_NAME);
			KLT.setDepth(BigDecimal.valueOf(0.6));
			KLT.setWidth(BigDecimal.valueOf(0.4));
			KLT.setWeight(BigDecimal.valueOf(30));
			KLT.setHeight(BigDecimal.valueOf(0.45));
			KLT.setAdditionalContent("KLT Behaelter 400 mm x 600 mm, bis 450 mm hoch, 30 ITEM_KG");
			em.persist(KLT);
		}

	}

	public void createStorageLocationsTyp() throws LocationTopologyException,
			BusinessObjectExistsException, BusinessObjectCreationException,
			BusinessObjectSecurityException {
		
		
		try {
			PALETTENPLATZ_TYP_2 = slTypeQuery
					.queryByIdentity(PALETTENPLATZ_TYP_2_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			// Typ Palettenplatz
			PALETTENPLATZ_TYP_2 = new LocationType();
			PALETTENPLATZ_TYP_2.setName(PALETTENPLATZ_TYP_2_NAME);
			em.persist(PALETTENPLATZ_TYP_2);
		}

		try {
			KOMMPLATZ_TYP = slTypeQuery.queryByIdentity(KOMMPLATZ_TYP_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			// Typ Kommplatz
			KOMMPLATZ_TYP = new LocationType();
			KOMMPLATZ_TYP.setName(KOMMPLATZ_TYP_NAME);
			em.persist(KOMMPLATZ_TYP);
		}
		
	}

	public void createCapacityConstraints() throws LocationTopologyException,
			BusinessObjectExistsException, BusinessObjectCreationException,
			BusinessObjectSecurityException {

		LocationType slTypeDefault = slTypeEntityService.getDefault();
		UnitLoadType PALETTE = ulTypeQuery.getDefaultUnitLoadType();
		UnitLoadType DUMMY_KOMM_ULTYPE = ulTypeQuery.getPickLocationUnitLoadType();
		
		EINE_PALETTE = null;
		try {
			EINE_PALETTE = typeCapacityConstaintQuery.queryByIdentity(EINE_PALETTE_NAME);
		} catch (BusinessObjectNotFoundException ex) {}
		
		if( EINE_PALETTE == null ) {
			EINE_PALETTE = typeCapacityConstaintService.getByTypes(slTypeDefault, PALETTE);
		}
		if( EINE_PALETTE == null ) {
			// Kapazitaet "unbegrenzt"
			EINE_PALETTE = new TypeCapacityConstraint();
//			EINE_PALETTE.setName(EINE_PALETTE_NAME);
			EINE_PALETTE.setUnitLoadType(PALETTE);
			EINE_PALETTE.setLocationType(slTypeDefault);
			EINE_PALETTE.setAllocation( new BigDecimal(100) );
			em.persist(EINE_PALETTE);
		}

		KOMM_FACH_DUMMY_LHM_CONSTR = null;
		try {
			KOMM_FACH_DUMMY_LHM_CONSTR = typeCapacityConstaintQuery
					.queryByIdentity(KOMM_FACH_DUMMY_LHM_CONSTR_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			KOMM_FACH_DUMMY_LHM_CONSTR = typeCapacityConstaintService.getByTypes(KOMMPLATZ_TYP, DUMMY_KOMM_ULTYPE);
		}
		if( KOMM_FACH_DUMMY_LHM_CONSTR == null ) {
			// Kapazitaet "unbegrenzt"
			KOMM_FACH_DUMMY_LHM_CONSTR = new TypeCapacityConstraint();
//			KOMM_FACH_DUMMY_LHM_CONSTR.setName(KOMM_FACH_DUMMY_LHM_CONSTR_NAME);
			KOMM_FACH_DUMMY_LHM_CONSTR.setUnitLoadType(DUMMY_KOMM_ULTYPE);
			KOMM_FACH_DUMMY_LHM_CONSTR.setLocationType(KOMMPLATZ_TYP);
			KOMM_FACH_DUMMY_LHM_CONSTR.setAllocation( new BigDecimal(100) );
			em.persist(KOMM_FACH_DUMMY_LHM_CONSTR);
		}
	}

	public void createAreas() throws LocationTopologyException,
			BusinessObjectExistsException, BusinessObjectCreationException,
			BusinessObjectSecurityException {
		try {
			STORE_AREA = areaQuery.queryByIdentity(STORE_AREA_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			STORE_AREA = new Area();
			STORE_AREA.setName(STORE_AREA_NAME);
			STORE_AREA.setUseFor(AreaUsages.STORAGE, true);
			em.persist(STORE_AREA);
		}

		try {
			KOMM_AREA = areaQuery.queryByIdentity(KOMM_AREA_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			KOMM_AREA = new Area();
			KOMM_AREA.setName(KOMM_AREA_NAME);
			KOMM_AREA.setUseFor(AreaUsages.PICKING, true);
			em.persist(KOMM_AREA);
		}

		try {
			WE_BEREICH = areaQuery.queryByIdentity(WE_BEREICH_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			WE_BEREICH = new Area();
			WE_BEREICH.setName(WE_BEREICH_NAME);
			WE_BEREICH.setUseFor(AreaUsages.GOODS_IN, true);
			em.persist(WE_BEREICH);
		}

		try {
			WA_BEREICH = areaQuery.queryByIdentity(WA_BEREICH_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			WA_BEREICH = new Area();
			WA_BEREICH.setName(WA_BEREICH_NAME);
			WA_BEREICH.setUseFor(AreaUsages.GOODS_OUT, true);
			em.persist(WA_BEREICH);
		}
		
		try {
			CLEARING_BEREICH = areaQuery.queryByIdentity(CLEARING_BEREICH_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			CLEARING_BEREICH = new Area();
			CLEARING_BEREICH.setName(CLEARING_BEREICH_NAME);
			em.persist(CLEARING_BEREICH);
		}
		
		try {
			PRODUCTION_BEREICH = areaQuery.queryByIdentity(PRODUCTION_BEREICH_NAME);
		} catch (BusinessObjectNotFoundException ex) {
			PRODUCTION_BEREICH = new Area();
			PRODUCTION_BEREICH.setName(PRODUCTION_BEREICH_NAME);
			em.persist(PRODUCTION_BEREICH);
		}
	}

	public void createStorageLocations() throws LocationTopologyException,
			BusinessObjectExistsException, BusinessObjectCreationException,
			BusinessObjectSecurityException {

		LocationType slTypeNorestriction = slTypeEntityService.getSystem();

		try {
			SL_WE = slQuery.queryByIdentity(TESTCLIENT,SL_WE_TESTCLIENT_NAME).get(0);
		} catch (BusinessObjectNotFoundException ex) {
			SL_WE = new StorageLocation();
			SL_WE.setName(SL_WE_TESTCLIENT_NAME);
			SL_WE.setClient(TESTCLIENT);
			SL_WE.setType(slTypeNorestriction);
			SL_WE.setArea(WE_BEREICH);
			em.persist(SL_WE);
		}

		try {
			SL_WA = slQuery.queryByIdentity(TESTCLIENT, SL_WA_TESTCLIENT_NAME).get(0);
		} catch (BusinessObjectNotFoundException ex) {
			SL_WA = new StorageLocation();
			SL_WA.setName(SL_WA_TESTCLIENT_NAME);
			SL_WA.setClient(TESTCLIENT);
			SL_WA.setType(slTypeNorestriction);
			SL_WA.setArea(WA_BEREICH);
			em.persist(SL_WA);
		}
		
		try {
			SL_PRODUCTION = slQuery.queryByIdentity(TESTCLIENT, SL_PRODUCTION_TESTCLIENT_NAME).get(0);
		} catch (BusinessObjectNotFoundException ex) {
			SL_PRODUCTION = new StorageLocation();
			SL_PRODUCTION.setName(SL_PRODUCTION_TESTCLIENT_NAME);
			SL_PRODUCTION.setClient(TESTCLIENT);
			SL_PRODUCTION.setType(slTypeNorestriction);
			SL_PRODUCTION.setArea(PRODUCTION_BEREICH);
			em.persist(SL_PRODUCTION);
		}
		
		try {
			SL_WE = slQuery.queryByIdentity(TESTMANDANT,SL_WE_TESTMANDANT_NAME).get(0);
		} catch (BusinessObjectNotFoundException ex) {
			SL_WE = new StorageLocation();
			SL_WE.setName(SL_WE_TESTMANDANT_NAME);
			SL_WE.setClient(TESTMANDANT);
			SL_WE.setType(slTypeNorestriction);
			SL_WE.setArea(WE_BEREICH);
			em.persist(SL_WE);
		}

		try {
			SL_WA = slQuery.queryByIdentity(TESTMANDANT, SL_WA_TESTMANDANT_NAME).get(0);
		} catch (BusinessObjectNotFoundException ex) {
			SL_WA = new StorageLocation();
			SL_WA.setName(SL_WA_TESTMANDANT_NAME);
			SL_WA.setClient(TESTMANDANT);
			SL_WA.setType(slTypeNorestriction);
			SL_WA.setArea(WA_BEREICH);
			em.persist(SL_WA);
		}
		
		try {
			SL_PRODUCTION = slQuery.queryByIdentity(TESTMANDANT, SL_PRODUCTION_TESTMANDANT_NAME).get(0);
		} catch (BusinessObjectNotFoundException ex) {
			SL_PRODUCTION = new StorageLocation();
			SL_PRODUCTION.setName(SL_PRODUCTION_TESTMANDANT_NAME);
			SL_PRODUCTION.setClient(TESTMANDANT);
			SL_PRODUCTION.setType(slTypeNorestriction);
			SL_PRODUCTION.setArea(PRODUCTION_BEREICH);
			em.persist(SL_PRODUCTION);
		}
		
	}

	public void createRacks() throws LocationTopologyException,
			BusinessObjectExistsException, BusinessObjectCreationException,
			BusinessObjectSecurityException, BusinessObjectNotFoundException,
			BusinessObjectModifiedException, BusinessObjectMergeException {
		
		LocationType slTypeDefault = slTypeEntityService.getDefault();
		UnitLoadType DUMMY_KOMM_ULTYPE = ulTypeQuery.getPickLocationUnitLoadType();
		
		TEST_RACK_1 = TEST_RACK_1_NAME;
		TEST_RACK_2 = TEST_RACK_2_NAME;

		for (int x = 1; x < 5; x++) {
			for (int y = 1; y < 4; y++) {
				StorageLocation rl;
				String locName = TEST_RACK_1 + "-1-" + y + "-" + x;
				try {
					rl = slQuery.queryByIdentity(locName);
				} catch (BusinessObjectNotFoundException ex) {
					rl = new StorageLocation();
					rl.setClient(TESTCLIENT);
					rl.setArea(KOMM_AREA);
					rl.setName(locName);
					rl.setRack(TEST_RACK_1);
					rl.setLocationType(KOMMPLATZ_TYP);
					rl.setXPos(x);
					rl.setYPos(y);
					em.persist(rl);


					UnitLoad ul = new UnitLoad();
					ul.setClient(TESTCLIENT);
					ul.setLabelId(locName);
					ul.setUnitLoadType(DUMMY_KOMM_ULTYPE);
					ul.setStorageLocation(rl);
					em.persist(ul);
				}
			}
		}

		for (int x = 1; x < 5; x++) {
			for (int y = 4; y < 6; y++) {
				StorageLocation rl;
				String locName = TEST_RACK_1 + "-1-" + y + "-" + x;
				try {
					rl = slQuery.queryByIdentity(locName);
				} catch (BusinessObjectNotFoundException ex) {
					rl = new StorageLocation();
					rl.setClient(TESTCLIENT);
					rl.setArea(STORE_AREA);
					rl.setName(locName);
					rl.setRack(TEST_RACK_1);
					rl.setLocationType(slTypeDefault);
					rl.setXPos(x);
					rl.setYPos(y);
					em.persist(rl);

				}
			}
		}
		
		for (int x = 1; x < 3; x++) {
			for (int y = 1; y < 4; y++) {
				StorageLocation rl;
				String locName = TEST_RACK_2 + "-1-" + y + "-" + x;
				try {
					rl = slQuery.queryByIdentity(locName);
				} catch (BusinessObjectNotFoundException ex) {
					rl = new StorageLocation();
					rl.setClient(TESTMANDANT);
					rl.setArea(KOMM_AREA);
					rl.setName(locName);
					rl.setRack(TEST_RACK_2);
					rl.setType(KOMMPLATZ_TYP);
					rl.setXPos(x);
					rl.setYPos(y);
					em.persist(rl);


					UnitLoad ul = new UnitLoad();
					ul.setClient(TESTMANDANT);
					ul.setLabelId(locName);
					ul.setType(DUMMY_KOMM_ULTYPE);
					ul.setStorageLocation(rl);
					em.persist(ul);
				}
			}
		}

		for (int x = 1; x < 3; x++) {
			for (int y = 4; y < 6; y++) {
				StorageLocation rl;
				String locName = TEST_RACK_2 + "-1-" + y + "-" + x;
				try {
					rl = slQuery.queryByIdentity(locName);
				} catch (BusinessObjectNotFoundException ex) {
					rl = new StorageLocation();
					rl.setClient(TESTMANDANT);
					rl.setArea(STORE_AREA);
					rl.setName(locName);
					rl.setRack(TEST_RACK_2);
					rl.setType(slTypeDefault);
					rl.setXPos(x);
					rl.setYPos(y);
					em.persist(rl);

				}
			}
		}
		

	}

	// ------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void remove(Class<BasicEntity> clazz) throws LocationTopologyException {

		try {
			List<BasicEntity> l;
			l = em.createQuery("SELECT o FROM " + clazz.getName() + " o")
					.getResultList();
			for (Iterator<BasicEntity> iter = l.iterator(); iter.hasNext();) {
				BasicEntity element = iter.next();
				element = (BasicEntity) em.find(clazz, element.getId());
				em.remove(element);
			}
			em.flush();

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			throw new LocationTopologyException();
		}
	}

	private void initClient() throws LocationTopologyException{
		try {
			TESTCLIENT = clientQuery.queryByIdentity(CommonTestTopologyRemote.TESTCLIENT_NUMBER);
			TESTCLIENT = em.find(Client.class, TESTCLIENT.getId());
			
			TESTMANDANT = clientQuery.queryByIdentity(CommonTestTopologyRemote.TESTMANDANT_NUMBER);
			TESTMANDANT = em.find(Client.class, TESTMANDANT.getId());
			
		}catch (Throwable e) {
			log.error(e.getMessage(), e);
			throw new LocationTopologyException();
		}		
	}

	public void clear() throws LocationTopologyException {
		try {
			initClient();

			clearUnitLoads();
			clearStorageLocations();
			
		} catch (LocationTopologyException ex) {
			throw ex;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			throw new LocationTopologyException();
		}
	}
	
	public void clearStorageLocations() throws LocationTopologyException {
		// Delete StorageLocations

		initClient();
		try {
			QueryDetail d = new QueryDetail(0, Integer.MAX_VALUE);
			TemplateQueryWhereToken t = new TemplateQueryWhereToken(
					TemplateQueryWhereToken.OPERATOR_EQUAL, "itemData.client",
					TESTCLIENT);
			TemplateQueryWhereToken t2 = new TemplateQueryWhereToken(
					TemplateQueryWhereToken.OPERATOR_EQUAL, "itemData.client",
					TESTMANDANT);
			t2.setParameterName("client2");t2.setLogicalOperator(TemplateQueryWhereToken.OPERATOR_OR);
			TemplateQuery q = new TemplateQuery();
			q.addWhereToken(t);
			q.addWhereToken(t2);
			q.setBoClass(FixAssignment.class);

			List<FixAssignment> l = assQuery.queryByTemplate(d, q);
			for (FixAssignment u : l) {
				u = em.find(FixAssignment.class, u.getId());
				em.remove(u);
			}
		} catch (Throwable e) {
			log.error(e, e);
			throw new LocationTopologyException();
		}
		
		try {

			QueryDetail d = new QueryDetail(0, Integer.MAX_VALUE);
			TemplateQueryWhereToken t = new TemplateQueryWhereToken(
					TemplateQueryWhereToken.OPERATOR_EQUAL, "client",
					TESTCLIENT);
			
			TemplateQueryWhereToken t2 = new TemplateQueryWhereToken(
					TemplateQueryWhereToken.OPERATOR_EQUAL, "client",
					TESTMANDANT);
			t2.setParameterName("client2");t2.setLogicalOperator(TemplateQueryWhereToken.OPERATOR_OR);
						
			TemplateQuery q = new TemplateQuery();
			q.setDistinct(true);
			q.addWhereToken(t);
			q.addWhereToken(t2);
			
			q.setBoClass(StorageLocation.class);

			List<StorageLocation> l = slQuery.queryByTemplate(d, q);
			
			//l.add(slQuery.queryByIdentity(SL_WA_NAME));
			
			for (StorageLocation rl : l) {
				try {
					
					rl = em.find(StorageLocation.class, rl.getId());
					for (UnitLoad u : rl.getUnitLoads()) {
						u = ulQuery.queryById(u.getId());
						u = em.find(UnitLoad.class, u.getId());
						for (StockUnit su : u.getStockUnitList()) {
							su = em.find(StockUnit.class, su.getId());
							em.remove(su);
						}
						em.remove(u);
					}
	
				} catch (Throwable ex) {
					log.error(ex.getMessage(), ex);
				}
				log.info("Remove: " + rl.getName());
				em.remove(rl);
			}
			
			em.flush();
		} catch (Throwable e) {
			log.error(e, e);
			throw new LocationTopologyException();
		}
	}

	public void clearUnitLoads() throws LocationTopologyException {
		try {
			QueryDetail d = new QueryDetail(0, Integer.MAX_VALUE);
			TemplateQueryWhereToken t = new TemplateQueryWhereToken(
					TemplateQueryWhereToken.OPERATOR_EQUAL, "client",
					TESTCLIENT);
			TemplateQueryWhereToken t2 = new TemplateQueryWhereToken(
					TemplateQueryWhereToken.OPERATOR_EQUAL, "client",
					TESTMANDANT);
			t2.setParameterName("client2");t2.setLogicalOperator(TemplateQueryWhereToken.OPERATOR_OR);
			TemplateQuery q = new TemplateQuery();
			q.addWhereToken(t);
			q.addWhereToken(t2);
			q.setBoClass(UnitLoad.class);

			List<UnitLoad> l = ulQuery.queryByTemplate(d, q);
			for (UnitLoad u : l) {
				u = em.find(UnitLoad.class, u.getId());
				em.remove(u);
			}
			em.flush();
		} catch (Throwable e) {
			log.error(e, e);
			throw new LocationTopologyException();
		}
	}
}
