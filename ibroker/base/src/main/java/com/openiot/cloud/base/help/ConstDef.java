/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

public class ConstDef {
  /** PORT */
  public static final String DATA_HOSTNAME = "127.0.0.1";

  public static final int DATA_PORT = 5685;
  public static final String DS_HOSTNAME = "127.0.0.1";
  public static final int DS_PORT = 61617;
  public static final String FOLDER_HOSTNAME = "127.0.0.1";
  public static final int FOLDER_PORT = 61617;
  public static final String META_HOSTNAME = "127.0.0.1";
  public static final int META_PORT = 61617;
  public static final String MQ_HOSTNAME = "127.0.0.1";
  public static final int MQ_PORT = 61616;
  public static final String OPT_HOSTNAME = "127.0.0.1";
  public static final int OPT_PORT = 61617;
  public static final String RD_HOSTNAME = "127.0.0.1";
  public static final int RD_PORT = 5684;

  /** URI and URI SEGMENT */
  public static final String DATA_URI = "/dp";

  public static final String DATA_URI_SEGMENT = "dp";
  public static final String DS_URI = "devsession";
  public static final String FOLDER_URI = "folder";
  public static final String META_URI = "meta";
  public static final String MQ_URL = "tcp://127.0.0.1:61616";
  public static final String OPT_URI = "opt";
  public static final String RD_URI = "/rd";
  public static final String RD_URI_SEGMENT = "rd";
  public static final String U_RD = "rd";
  public static final String U_GRP = "group";
  public static final String U_DEV = "device";
  public static final String U_RES = "resource";
  public static final String U_GRPTYPE = "grptype";
  // resource property
  public static final String U_RESPRO = "respro";
  public static final String U_OPERATION = "operation";
  // device session
  public static final String U_DEVSESS = "session";
  public static final String U_META = "meta";
  public static final String U_RESTYPE = "restype";
  public static final String U_FOLDER = "folder";
  // dp
  public static final String U_DP = "dp";
  public static final String U_RAW = "raw";
  public static final String U_PRODATA = "prop";
  public static final String U_RESDATA = "res";
  public static final String U_DSS = "dss";
  public static final String U_ATTR = "attr";
  public static final String U_CFG = "cfg";
  public static final String U_DST = "dst";
  public static final String U_MBR = "mbr";

  public static final String U_USER = "/api/user";
  public static final String U_USER_LOGIN = "/api/user/login";
  public static final String U_USER_REFRESH = "/api/user/refresh";
  public static final String U_USER_SELECTPROJECT = "/api/user/selectproject";
  public static final String U_USER_VALIDATION = "/api/user/validation";
  public static final String U_PROJECT = "/api/project";
  public static final String U_PROJECT_MEMBER = "/api/project/member";
  public static final String U_PROJECT_ATTR = "/api/project/attr";
  public static final String U_PROJECT_CFG = "/api/project/cfg";

  /** ILINK MESSAGE FIELDS */
  // iagent id
  public static final String FH_K_AID = "_aid";
  // endpoint name
  public static final String FH_K_EP = "_ep";
  // response code
  public static final String FH_K_REP = "_re";

  public static final String FH_K_TAG = "_tag";

  public static final String FH_K_TIME = "_tm";
  // handshake
  public static final String FH_V_HAN1 = "handshake1";
  public static final String FH_V_HAN2 = "handshake2";

  // clock synchronization
  public static final String FH_K_SECONDS = "_epoch_seconds";
  public static final String FH_K_MICRO = "_micro";

  // values
  public static final String FH_V_PRO = "key-prov";

  public static final String FH_V_PING = "ping";

  // require 2nd
  public static final int FH_V_REQ2HAN = 100;
  // succeed
  public static final int FH_V_SUCC = 1;
  // fail
  public static final int FH_V_FAIL = 0;

  public static final int ILINK_PORT = 5000;
  // 20s
  public static final int ILINK_TIMEOUT = 20000;
  public static final int COAP_TIMEOUT = 20000;

  public static final int MAX_COMMUNICATION_FAILURE = 3;

  /** JMS MESSAGE FIELDS */
  public static final String JMS_MSG_KEY_ONLINE_IAGENT = "online_iagent";

  public static final String JMS_MSG_KEY_ACTION = "action";
  public static final String JMS_MSG_KEY_PAYLOAD = "payload";
  public static final String JMS_MSG_KEY_PAYLOAD_FMT = "playload_format";
  public static final String JMS_MSG_KEY_STATUS = "status";
  public static final String JMS_MSG_KEY_EXTRA = "extra";
  public static final String MSG_KEY_USR = "usr";
  public static final String MSG_KEY_PRJ = "prj";
  public static final String MSG_KEY_ROLE = "role";

  public static final String JMS_MSG_KEY_RESPONSECODE = "responsecode";
  public static final String JMS_MSG_KEY_URI = "uri";
  public static final String JMS_MSG_VALUE_PAYLOAD_FMT = "coap";

  public static final String JMS_MSG_KEY_NAME = "n";
  public static final String JMS_MSG_KEY_TYPE = "t";
  public static final String JMS_MSG_KEY_TARGET_ID = "ti";
  public static final String JMS_MSG_KEY_TARGET_TYPE = "tt";
  public static final String JMS_MSG_KEY_SRV_PROVIDER = "sp";
  public static final String JMS_MSG_KEY_PARAM = "p";
  public static final String JMS_MSG_KEY_CRATE_TIME = "tm";

  public static final String JMSTYPE_REQUEST = "request";
  public static final String JMSTYPE_RESPONSE = "response";

  public static final String JMS_MSG_KEY_UUID = "UUID";

  /** JMS TOPIC and QUEUE */
  public static final String MQ_QUEUE_OPT_REQUEST = "/opt";

  public static final String MQ_TOPIC_ONLINE_IAGENT = "/online/iagent";
  public static final String MQ_QUEUE_PROV_AUTH = "/prov/auth";
  public static final String MQ_QUEUE_PROV_INFO = "/prov/info";
  public static final String MQ_QUEUE_PROV_RESET = "/prov/reset";
  public static final String MQ_QUEUE_PROV_REPLACE = "/prov/replace";
  public static final String MQ_QUEUE_PROV_PROJECT = "/prov/project";

  /** mongo db collections */
  public static final String C_GRPTYPE = "GroupType";

  public static final String C_GRP = "Group";
  public static final String C_DEVTYPE = "DeviceType";
  public static final String C_DEV = "Device";
  public static final String C_RESTYPE = "ResourceType";
  public static final String C_RES = "Resource";
  public static final String C_RESPRO = "ResProperty";
  public static final String C_DEVSESS = "DevSession";
  public static final String C_RAWDATA = "RawData";
  public static final String C_PRODATA = "ProData";
  public static final String C_CONFIG = "Config";
  public static final String C_FOLDER = "Folder";
  public static final String C_FLDDEV = "Folder_Device";
  public static final String C_USERDATA = "UserData";
  public static final String C_TASK = "Task";
  public static final String C_TASKSRVREG = "TaskServiceRegistry";
  public static final String C_STATSDATA = "StatsData";
  public static final String C_STATSCFG = "StatsConfig";
  public static final String C_USER = "Users";
  public static final String C_PROJECT = "Projects";
  public static final String C_FACTORYKEY = "FactoryKey";
  public static final String C_GATEWAY = "Gateway";

  // end mongo db collections

  /** mongo and JSON fiedds */
  public static final String F_ID = "_id";

  public static final String F_IID = "id";
  public static final String F_NAME = "n";
  public static final String F_DISPLAYNAME = "dpn";
  public static final String F_VALUE = "d";
  public static final String F_TYPE = "t";

  public static final String F_DESCRIPTION = "d";
  public static final String F_REFLIST = "rl";
  public static final String F_FROM = "f";
  public static final String F_TO = "t";
  // entities
  public static final String F_ENTS = "ms";
  public static final String F_REFID = "mid";
  public static final String F_REFTYPE = "mt";
  public static final String F_REFFURL = "murl";

  public static final String F_MBR = "mbr";
  public static final String F_MD = "md";
  public static final String F_MR = "mr";
  public static final String F_DEVID = "di";
  public static final String F_RESURI = "res";
  public static final String F_DATATYPE = "dt";
  public static final String F_HELPERSTR = "h";

  // attributes
  public static final String F_ATTRS = "as";
  public static final String F_ATTRNAME = "an";
  public static final String F_ATTRVALUE = "av";
  public static final String F_ATTRTIMESTAMP = "ats";
  // configuration
  public static final String F_CONFIGS = "cs";
  public static final String F_CONFIGNAME = "cn";
  public static final String F_CONFIGVALUE = "cv";
  public static final String F_CONFIGTIMESTAMP = "cts";
  // datasource
  public static final String F_DATASOURCES = "dss";
  public static final String F_DATASOURCENAME = "dsn";
  public static final String F_DATASOURCETYPE = "dst";
  public static final String F_DATASOURCETABLE = "table";
  public static final String F_DATASOURCEREF = "ref";
  public static final String F_DATASOURCEINTLID = "dsintId";
  public static final String F_DATASOURCEREFS = "dsrs";
  public static final String F_DATASOURCEDEFS = "dsdefs";

  public static final String F_DATASOURCEREFNAME = "dsrurl";
  public static final String F_DATASOURCEREFFROM = "dsrf";
  public static final String F_DATASOURCEREFTO = "dsrt";
  public static final String F_DSRESPROPGLOBALID = "dsri";

  public static final String F_DATASOURCEIID = "dsiid";
  public static final String F_DATASOURCETIMESTAMP = "dsts";
  public static final String F_DATASOURCEDATA = "dsdat";
  public static final String F_DATASOURCEDATATYPE = "dsdttp";
  public static final String F_DATASOURCELTSD = "dsltsd";

  // user configration
  public static final String F_USERCFG = "uc";
  // device
  public static final String F_DEV = "d";
  // resource
  public static final String F_RES = "res";
  // property
  public static final String F_PROPERTY = "p";
  // iagent
  public static final String F_IAGENT = "ia";
  // ibroker
  public static final String F_IBROKER = "ib";
  // folder
  public static final String F_FOLDER = "f";
  // data life
  public static final String F_DATALIFE = "dl";
  public static final String F_TIME = "t";
  public static final String F_DATA = "d";
  // used in fields filter
  public static final String F_GRPS = "grps";
  public static final String F_DEVNAME = "dn";
  // end used in fields filter
  // group type
  // default value
  public static final String F_DFLT = "d";
  // group relevant
  public static final String F_GRPTYPE = "gt";
  public static final String F_GRPTYPEDPN = "gtdpn";
  // device type
  public static final String F_RESTYPES = "rs";
  // device relevant
  // standard
  public static final String F_STAND = "st";
  // device type
  public static final String F_DEVTYPE = "dt";
  // connected
  public static final String F_CONNED = "c";
  // enabled
  public static final String F_ENABLED = "e";
  // resouces
  public static final String F_RESES = "rs";
  // refresh number
  public static final String F_RFRNUM = "rn";
  // resource type
  // description
  public static final String F_DSCRB = "d";
  // resource title
  public static final String F_TITLE = "ttle";
  // property definition
  public static final String F_PRODEFS = "ps";
  // short name
  public static final String F_SHTNAME = "s";
  // mandatory
  public static final String F_MAND = "m";
  // resource relevant
  // url
  public static final String F_URL = "u";
  // resource type
  public static final String F_RESTYPE = "rt";
  // properties
  public static final String F_PROES = "ps";
  // resource property relevant
  // access
  public static final String F_ACCESS = "a";
  // implemented
  public static final String F_IMPLED = "i";
  // unit
  public static final String F_UNIT = "u";
  // last data
  public static final String F_LASTDATA = "ld";
  // last time
  public static final String F_LASTTIME = "lt";
  // device session relevant
  // begin time
  public static final String F_BEGEIN = "b";
  // end time
  public static final String F_END = "e";
  // raw data entity's url
  public static final String F_FULLURL = "fu";
  // format
  public static final String F_FORMAT = "f";
  // property data
  public static final String F_RAWDATA = "r";
  // config
  // target type
  public static final String F_TGTTYPE = "tt";
  // target id
  public static final String F_TGTID = "ti";

  // folder relevant
  // folder type
  public static final String F_FLDTYPE = "ft";
  public static final String F_PARENT = "p";
  // folder depth
  public static final String F_DEPTH = "d";

  public static final String F_GTDETAIL = "gtdtl";

  public static final String F_LOOKUP_AS = "dtl";

  /** task relevant */
  // task type
  public static final String F_TSKTYPE = "t";
  // task service id
  public static final String F_TSKSRVID = "srvid";
  // task target type
  public static final String F_TSKTGTTYPE = "tt";
  // task target id
  public static final String F_TSKTGTID = "ti";
  // service provider URL
  public static final String F_SRVPRVD = "sp";
  // task parameter
  public static final String F_PARAM = "p";
  // task execution time
  public static final String F_TSKTIME = "tm";
  // task sent flag
  public static final String F_TSKSENTFLG = "s";
  // task fail flag
  public static final String F_TSKFAILFLG = "f";
  // task failed handler
  public static final String F_FAILHANDLE = "fh";
  // task failed retry interval
  public static final String F_RETRYINTER = "ri";
  // task failed retry times
  public static final String F_RETRYTIMES = "rt";
  // task failed retry counter
  public static final String F_RETRYCOUNT = "rtc";
  // task failed last retry time
  public static final String F_RETRYLASTTIME = "rlt";

  /** task service registration relevant */
  // task array
  public static final String F_TSKARR = "ts";
  // description
  public static final String F_DES = "des";
  // task scheduler type
  public static final String F_SCHEDULERTYPE = "st";
  // delay
  public static final String F_DELAY = "delay";
  // overwrite
  public static final String F_OVERWRITE = "o";

  // statics fields
  public static final String F_STATTARGETTYPE = "tt";
  public static final String F_STATTARGETID = "ti";
  public static final String F_STATS = "stats";
  public static final String F_PEROIDLIST = "ps";
  public static final String F_PEROID = "p";
  public static final String F_STD = "std";
  public static final String F_CUSTOM = "cust";
  public static final String F_STATNAME = "n";
  public static final String F_STATDES = "des";
  public static final String F_STATTYPE = "t";
  public static final String F_STATQUERY = "q";
  public static final String F_STATTIME = "tm";
  public static final String F_STATSDATA = "s";
  public static final String F_STATVALUE = "v";
  public static final String F_LASTSTATTIME = "ltm";

  public static final String F_COUNT = "cnt";
  public static final String F_MIN = "min";
  public static final String F_MAX = "max";
  public static final String F_AVG = "avg";
  public static final String F_SUM = "sum";
  public static final String F_SW = "sw";
  public static final String F_DUR = "dur";
  public static final String F_DURMAX = "dur_max";
  public static final String F_DURMIN = "dur_min";
  public static final String F_DURAVG = "dur_avg";
  public static final String F_RANGECOUNT = "rngc";

  public static final String F_PROPNAME = "pt";
  public static final String F_DP = "dp";
  public static final String F_PROJECT = "prj";
  public static final String F_ROLES = "roles";

  public static final String F_GROUP = "grp";

  public static final String F_CLASS = "c";
  public static final String F_THRESHOLD_LOW = "t_l";
  public static final String F_THRESHOLD_HIGH = "t_h";
  public static final String F_REPORT_INTERVAL = "interval";
  public static final String F_OPERATE = "operate";
  public static final String F_OP_TYPE = "type";
  public static final String F_OP_BG_STATE = "background_state";
  public static final String F_OP_DI = "di";
  public static final String F_OP_URL = "url";
  public static final String F_OP_PN = "pn";
  public static final String F_OP_SCHD = "sched";
  public static final String F_OP_ST_CMDS = "state_cmds";
  public static final String F_OP_REPEAT = "repeat";
  public static final String F_PROV_IAGENTID = "iAgentId";
  public static final String F_PROV_SERIALNUM = "serialNum";
  // end fields

  /** query parameter */
  public static final String Q_SEARCH = "search";

  public static final String Q_PAGE = "page";
  public static final String Q_LIMIT = "limit";
  public static final String Q_FIELDS = "fields";
  public static final String Q_ID = "id";
  public static final String Q_NAME = "name";
  public static final String Q_GRP = "grp";
  public static final String Q_DEV = "device";
  // used by gateway side
  public static final String Q_DEVID = "di";
  public static final String Q_RES = "resource";
  public static final String Q_RESURI = "res";

  public static final String Q_PRO = "property";
  // resource type
  public static final String Q_RT = "rt";
  public static final String Q_TIME = "time";
  // group type id
  public static final String Q_GTID = "gtid";
  // group type name
  public static final String Q_GTNAME = "gtname";

  public static final String Q_GT = "gt";
  public static final String Q_ATTRS = "attrs";
  public static final String Q_CFGS = "cfgs";
  public static final String Q_DSN = "dsn";
  public static final String Q_MD = "md";
  public static final String Q_MR = "mr";
  public static final String Q_GROUP = "group";
  public static final String Q_DISPLAY = "display";

  // entity name
  public static final String Q_ENTTYPE = "enttype";
  // entity id
  public static final String Q_ENTID = "entid";
  // device relevant
  public static final String Q_STANDARD = "standard";
  // device type id
  public static final String Q_DTID = "dtid";
  // device type name
  public static final String Q_DTNAME = "dt";
  // iagent device id
  public static final String Q_AGENT = "agent";
  public static final String Q_CONNECTED = "connected";
  public static final String Q_ENABLED = "enabled";
  // resource relevant
  public static final String Q_URL = "url";
  public static final String Q_FURL = "furl";
  // resource type id
  public static final String Q_RTID = "rtid";
  // resource type name
  public static final String Q_RTNAME = "rtname";
  public static final String Q_OBSERVE = "observe";
  public static final String Q_RESURL = "resurl";
  public static final String Q_IMPLED = "implemented";
  public static final String Q_SESID = "s";
  public static final String Q_ENDED = "ended";
  public static final String Q_FROM = "from";
  public static final String Q_TO = "to";
  public static final String Q_RECENTSECONDS = "recent_seconds";
  // raw data
  // public static final String Q_FULLURL = "url";
  // pro data
  public static final String Q_RAW = "raw";

  public static final String Q_PROPNAME = "pt";
  public static final String Q_RESGLOBALID = "ri";

  public static final String Q_PRDNAME = "product_name";
  public static final String Q_PATH = "path_name";
  public static final String Q_TGTTYPE = "target_type";
  public static final String Q_TGTID = "target_id";

  public static final String Q_FOLDER = "folder";
  public static final String Q_ANCESTOR = "ancestor";
  public static final String Q_LEAF = "leaf";
  public static final String Q_PARENT = "parent";

  public static final String Q_WITH_ATTRS = "with_attrs";
  public static final String Q_WITH_CFGS = "with_cfgs";
  public static final String Q_WITH_DEVS = "with_devs";
  public static final String Q_WITH_DSS = "with_dss";
  public static final String Q_WITH_RESES = "with_reses";

  public static final String Q_PREFIX_ATTR = "attr";
  public static final String Q_PREFIX_CFG = "cfg";
  public static final String Q_DATASOURCENAME = "dsn";
  public static final String Q_DATASOURCETYPE = "dst";

  public static final String Q_PERIOD = "p";
  public static final String Q_STAT = "stat";
  public static final String Q_CUST = "cust";

  public static final String Q_PROJECT = "project";

  public static final String Q_UNIT = "unit";
  public static final String Q_TITLE = "title";
  public static final String Q_CLASS = "class";
  public static final String Q_DESCRIPTION = "description";
  public static final String Q_THRESHOLD_LOW = "thres_low";
  public static final String Q_THRESHOLD_HIGH = "thres_high";

  public static final String Q_PRIMARY_IAGENT = "set-primary";
  public static final String Q_DEVICE_PLAN = "device_plan";
  public static final String Q_IS_GLOBAL = "global";
  public static final String Q_RESET = "reset";
  public static final String Q_IAGENTID = "aid";
  public static final String Q_PROJECTID = "pi";
  public static final String Q_RANDOM = "random";
  public static final String Q_PROJECTID_II = "project_id";
  public static final String Q_USER = "user";
  // end query parameter

  public static final String RT_ITEM_URI = "uripath";
  public static final String RT_ITEM_INETADDR = "inetaddr";

  // content format
  public static final String CF_TEXT = "text";
  public static final String CF_LWM2M = "lwm2m";
  public static final String CF_OCF = "ocf";
  public static final String CF_UNKOWN = "unknown";
  // end content format

  // access
  public static final String AC_READ = "r";
  public static final String AC_WRITE = "w";
  public static final String AC_EXE = "e";
  // end access

  // data type
  public static final String DT_BYTE = "byte";
  public static final String DT_INT = "int";
  public static final String DT_INTEGER = "integer";
  public static final String DT_FLOAT = "float";
  public static final String DT_BOOLEAN = "boolean";
  public static final String DT_STRING = "string";
  public static final String DT_OPAQUE = "opaque";
  // end data type

  // default page
  public static final int DFLT_PAGE = 0;
  // default page size
  public static final int DFLT_SIZE = 1000;
  // maximum page size
  public static final int MAX_SIZE = 3600;

  // group entity type
  public static final String ET_DEV = "dev";
  public static final String ET_RES = "res";
  // end group entity type

  // device standard
  public static final String S_LW = "lwm2m";
  public static final String S_OCF = "ocf";
  public static final String S_REST = "rest";
  public static final String S_PUB = "publish";
  // end device standard

  // end database/json fields

  // lwm2m+json
  public static final String LW_ELES = "e";
  public static final String LW_NAME = "n";
  // string value
  public static final String LW_STRVAL = "sv";
  // float vlaue
  public static final String LW_FLOVAL = "v";
  // boolean value
  public static final String LW_BOOVAL = "bv";
  // time
  public static final String LW_TIME = "t";
  // base time
  public static final String LW_BASETIME = "bt";
  // base name
  public static final String LW_BASENAME = "bn";
  // end lwm2m+json

  // download config relevant
  public static final String CFG_PRN_IAGENT = "iagent";
  public static final String CFG_TT_DEVONGW = "device_on_gateway";
  public static final Object CFG_TT_DEV = "device";
  public static final String CFG_TT_GRP = "group";
  public static final String CFG_TT_PRJ = "project";
  public static final String CFG_PTN_DEVCFG = "device.cfg";
  public static final String CFG_PTN_GRPCFG = "group.cfg";
  public static final String CFG_PTN_MDCFG = "modbus.cfg";
  public static final String CFG_PTN_PRJCFG = "project.cfg";
  // end download config relevant

  public static final String ROOT_FOLDER_ID = "root";
  public static final String DEV_TYPE_IAGENT = "intel.iagent";
  public static final String STANDARD_IAGENT = "iagent";

  // event monitor relevant
  public static final String EVENT_TYPE_NEW_DATA = "NEW_DATA";
  public static final String EVENT_TYPE_CFG_SYNC = "CFG_SYNC";
  public static final String EVENT_TYPE_DEV_STAT = "DEV_STAT";
  public static final String EVENT_TYPE_PROVISOIN = "PROVISION";
  public static final String EVENT_TYPE_FIRST_ONLINE = "FIRST_ONLINE";
  public static final String EVENT_TYPE_DEVICE_PROJECT = "DEVICE_PROJECT";

  public static final String EVENT_TARGET_TYPE_DEVICE = "DEVICE";
  public static final String EVENT_TARGET_TYPE_GROUP = "GROUP";
  public static final String EVENT_TARGET_TYPE_PROJECT = "PROJECT";

  public static final String EVENT_TASK_OPTION_APPEND = "APPEND";
  public static final String EVENT_TASK_OPTION_OVERWRITE = "OVERWRITE";
  public static final String EVENT_TASK_OPTION_IGNORE = "IGNORE";

  public static final String EVENT_MONITOR_RD_DEVICE_PROJECT = "RD_DEVICE_PROJECT";
  public static final String EVENT_MONITOR_AMS_DEVICE_PROJECT = "amsClientProjectIdChangeMonitor";
  // end download config relevant

  // Time const
  public static final int MINUTE_SECONDS = 60;
  public static final int HOUR_SECONDS = 60 * 60;
  public static final int DAY_SECONDS = 24 * 60 * 60;

  // for config and attribute
  public static final String KEY_O = "o";
  public static final String KEY_OMIN = "omin";
  public static final String V_TRUE = "true";
  public static final String V_OMIN_DEFAULE = "30";

  // somehing for test
  public static final String R_IM_PREFIX = "/importdb/";
  public static final String R_IM_SUFFIX = ".jsons";
  // AMS url
  public static final String URL_AMS_CFG_IDENDIFY =
      "http://%s/ams_user_cloud/ams/v1/config/identifier?product_name=%s&path_name=%s&target_type=%s";
  public static final String URL_AMS_CFG_INSTANCE =
      "http://%s/ams_user_cloud/ams/v1/config/instance?product_name=%s&path_name=%s&target_type=%s";
  public static final String V_STAR = "*";
  public static final String V_ROLE_USER = "user";
  public static final String V_ROLE_ADMIN = "admin";
  public static final String KEY_PID = "prj";

  public static final String REDIS_PREFIX_LATEST_DATA = "ld";
  public static final String REDIS_PREFIX_GROUP_DATA_SOURCE = "ds";
  public static final String REDIS_PREFIX_REFERENCE = "ds:ref";
  public static final String REDIS_PREFIX_REFERENCE_DEFINITION = "ref:def";

  public static final String F_DEVICE_PLAN = "device_plan";
  public static final String F_REFERRULE = "refrule";
  public static final String F_OPERATEATE = "operate";
  public static final String F_DSS_DEF_RULE_IAGENT = "<iagent>";
  public static final String F_DSS_DEF_RULE_DEVICE_PLAN = "$";
  public static final String U_DEVICE_PLAN = "deviceplan";
  public static final String F_ALARMTARGET = "tag";
  public static final String F_ALARMID = "alarmid";
  public static final String F_ALARMTAGTYPE = "targettype";
  public static final String F_ALARMTAGID = "targetid";
  public static final String F_ALARMDETAILS = "content";
  public static final String F_ALARMBEGINNINGTIME = "settime";
  public static final String F_ALARMENDTIME = "cleartime";
  public static final String F_ALARMSTATUS = "status";
  public static final String F_TAGGRP = "group";
  public static final String F_ALARMTITLE = "title";
  public static final String F_ALARMSEVERITY = "severity";
  public static final String F_UNITMONTH = "m";
  public static final String F_UNITWEEK = "w";
  public static final String F_UNITHOUR = "h";
  public static final String F_ALADEFGENECONDITION = "generation_condition";
  public static final String F_ALADEFCLEANCONDITION = "clean_condition";
  public static final String F_ALADEFTRIGGERACTION = "trigger_action";
  public static final String F_ALADEFCLEANACTION = "clean_action";
  public static final String F_ALADEFNOTES = "notes";
  public static final String F_PROP = "prop";
  public static final String F_DSN = "dsn";
  public static final String F_VALUE_INF = "value";
  public static final String F_PRIMARY_IAGENT = "_primary-iagent";
  public static final String F_HELPER = "help";
  public static final String F_TIMESTAMP = "time";
}
