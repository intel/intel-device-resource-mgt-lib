/*
 * command_handler.c
 *
 *  Created on: Nov 27, 2016
 *      Author: xwang98
 */


#include "lwm2m_server.h"

/* In the commandline.c, fprintf is used to output the log information
 * As we want avoid any change to upstream code, we may refer the link below
 * for redirect stderr into the debug socket
   http://stackoverflow.com/questions/8776103/how-to-redirect-fprintf-output-to-c-socket
*/

extern int g_quit;

static void prv_print_error(uint8_t status)
{
    CONSOLE_LOG( "Error: ");
    print_status(stdout, status);
    CONSOLE_LOG( "\r\n");
}

static void prv_quit(char * buffer,
                     void * user_data)
{
    g_quit = 1;
}

static char * prv_dump_binding(lwm2m_binding_t binding)
{
    switch (binding)
    {
    case BINDING_UNKNOWN:
        return "Not specified";
    case BINDING_U:
        return "UDP";
    case BINDING_UQ:
        return "UDP queue mode";
    case BINDING_S:
        return "SMS";
    case BINDING_SQ:
        return "SMS queue mode";
    case BINDING_US:
        return "UDP plus SMS";
    case BINDING_UQS:
        return "UDP queue mode plus SMS";
    default:
        return "";
    }
}

static void prv_dump_client(lwm2m_client_t * targetP)
{
    lwm2m_client_object_t * objectP;

    CONSOLE_LOG( "Client #%d:\r\n", targetP->internalID);
    CONSOLE_LOG( "\tname: \"%s\"\r\n", targetP->name);
    CONSOLE_LOG( "\tbinding: \"%s\"\r\n", prv_dump_binding(targetP->binding));
    if (targetP->msisdn) CONSOLE_LOG( "\tmsisdn: \"%s\"\r\n", targetP->msisdn);
    if (targetP->altPath) CONSOLE_LOG( "\talternative path: \"%s\"\r\n", targetP->altPath);
    CONSOLE_LOG( "\tlifetime: %d sec\r\n", targetP->lifetime);
    CONSOLE_LOG( "\tobjects: ");
    for (objectP = targetP->objectList; objectP != NULL ; objectP = objectP->next)
    {
        if (objectP->instanceList == NULL)
        {
            CONSOLE_LOG( "/%d, ", objectP->id);
        }
        else
        {
            lwm2m_list_t * instanceP;

            for (instanceP = objectP->instanceList; instanceP != NULL ; instanceP = instanceP->next)
            {
                CONSOLE_LOG( "/%d/%d, ", objectP->id, instanceP->id);
            }
        }
    }
    CONSOLE_LOG( "\r\n");
}

static void prv_output_clients(char * buffer,
                               void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    lwm2m_client_t * targetP;

    targetP = lwm2mH->clientList;

    if (targetP == NULL)
    {
        CONSOLE_LOG( "No client.\r\n");
        return;
    }

    for (targetP = lwm2mH->clientList ; targetP != NULL ; targetP = targetP->next)
    {
        prv_dump_client(targetP);
    }
}

static int prv_read_id(char * buffer,
                       uint16_t * idP)
{
    int nb;
    int value;

    nb = sscanf(buffer, "%d", &value);
    if (nb == 1)
    {
        if (value < 0 || value > LWM2M_MAX_ID)
        {
            nb = 0;
        }
        else
        {
            *idP = value;
        }
    }

    return nb;
}


static void prv_result_callback(uint16_t clientID,
                                lwm2m_uri_t * uriP,
                                int status,
                                lwm2m_media_type_t format,
                                uint8_t * data,
                                int dataLength,
                                void * userData)
{
    CONSOLE_LOG( "\r\nClient #%d /%d", clientID, uriP->objectId);
    if (LWM2M_URI_IS_SET_INSTANCE(uriP))
        CONSOLE_LOG( "/%d", uriP->instanceId);
    else if (LWM2M_URI_IS_SET_RESOURCE(uriP))
        CONSOLE_LOG( "/");
    if (LWM2M_URI_IS_SET_RESOURCE(uriP))
            CONSOLE_LOG( "/%d", uriP->resourceId);
    CONSOLE_LOG( " : ");
    print_status(stdout, status);
    CONSOLE_LOG( "\r\n");

    output_data(stdout, format, data, dataLength, 1);

    CONSOLE_LOG( "\r\n> ");
    fflush(stdout);
}

static void prv_notify_callback(uint16_t clientID,
                                lwm2m_uri_t * uriP,
                                int count,
                                lwm2m_media_type_t format,
                                uint8_t * data,
                                int dataLength,
                                void * userData)
{
    CONSOLE_LOG( "\r\nNotify from client #%d /%d", clientID, uriP->objectId);
    if (LWM2M_URI_IS_SET_INSTANCE(uriP))
        CONSOLE_LOG( "/%d", uriP->instanceId);
    else if (LWM2M_URI_IS_SET_RESOURCE(uriP))
        CONSOLE_LOG( "/");
    if (LWM2M_URI_IS_SET_RESOURCE(uriP))
            CONSOLE_LOG( "/%d", uriP->resourceId);
    CONSOLE_LOG( " number %d\r\n", count);

    output_data(stdout, format, data, dataLength, 1);

    CONSOLE_LOG( "\r\n> ");
    fflush(stdout);
}

static void prv_read_client(char * buffer,
                            void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char* end = NULL;
    int result;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_dm_read(lwm2mH, clientId, &uri, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}

static void prv_discover_client(char * buffer,
                                void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char* end = NULL;
    int result;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_dm_discover(lwm2mH, clientId, &uri, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}

static void prv_write_client(char * buffer,
                             void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char * end = NULL;
    int result;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    buffer = get_next_arg(end, &end);
    if (buffer[0] == 0) goto syntax_error;

    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_dm_write(lwm2mH, clientId, &uri, LWM2M_CONTENT_TEXT, (uint8_t *)buffer, end - buffer, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}


static void prv_time_client(char * buffer,
                            void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char * end = NULL;
    int result;
    lwm2m_attributes_t attr;
    int nb;
    int value;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    memset(&attr, 0, sizeof(lwm2m_attributes_t));
    attr.toSet = LWM2M_ATTR_FLAG_MIN_PERIOD | LWM2M_ATTR_FLAG_MAX_PERIOD;

    buffer = get_next_arg(end, &end);
    if (buffer[0] == 0) goto syntax_error;

    nb = sscanf(buffer, "%d", &value);
    if (nb != 1) goto syntax_error;
    if (value < 0) goto syntax_error;
    attr.minPeriod = value;

    buffer = get_next_arg(end, &end);
    if (buffer[0] == 0) goto syntax_error;

    nb = sscanf(buffer, "%d", &value);
    if (nb != 1) goto syntax_error;
    if (value < 0) goto syntax_error;
    attr.maxPeriod = value;

    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_dm_write_attributes(lwm2mH, clientId, &uri, &attr, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}


static void prv_attr_client(char * buffer,
                            void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char * end = NULL;
    int result;
    lwm2m_attributes_t attr;
    int nb;
    float value;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    memset(&attr, 0, sizeof(lwm2m_attributes_t));
    attr.toSet = LWM2M_ATTR_FLAG_LESS_THAN | LWM2M_ATTR_FLAG_GREATER_THAN;

    buffer = get_next_arg(end, &end);
    if (buffer[0] == 0) goto syntax_error;

    nb = sscanf(buffer, "%f", &value);
    if (nb != 1) goto syntax_error;
    attr.lessThan = value;

    buffer = get_next_arg(end, &end);
    if (buffer[0] == 0) goto syntax_error;

    nb = sscanf(buffer, "%f", &value);
    if (nb != 1) goto syntax_error;
    attr.greaterThan = value;

    buffer = get_next_arg(end, &end);
    if (buffer[0] != 0)
    {
        nb = sscanf(buffer, "%f", &value);
        if (nb != 1) goto syntax_error;
        attr.step = value;

        attr.toSet |= LWM2M_ATTR_FLAG_STEP;
    }

    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_dm_write_attributes(lwm2mH, clientId, &uri, &attr, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }

    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}

static void prv_clear_client(char * buffer,
                             void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char * end = NULL;
    int result;
    lwm2m_attributes_t attr;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    memset(&attr, 0, sizeof(lwm2m_attributes_t));
    attr.toClear = LWM2M_ATTR_FLAG_LESS_THAN | LWM2M_ATTR_FLAG_GREATER_THAN | LWM2M_ATTR_FLAG_STEP | LWM2M_ATTR_FLAG_MIN_PERIOD | LWM2M_ATTR_FLAG_MAX_PERIOD ;

    buffer = get_next_arg(end, &end);
    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_dm_write_attributes(lwm2mH, clientId, &uri, &attr, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}


static void prv_exec_client(char * buffer,
                            void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char * end = NULL;
    int result;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    buffer = get_next_arg(end, &end);


    if (buffer[0] == 0)
    {
        result = lwm2m_dm_execute(lwm2mH, clientId, &uri, 0, NULL, 0, prv_result_callback, NULL);
    }
    else
    {
        if (!check_end_of_args(end)) goto syntax_error;

        result = lwm2m_dm_execute(lwm2mH, clientId, &uri, LWM2M_CONTENT_TEXT, (uint8_t *)buffer, end - buffer, prv_result_callback, NULL);
    }

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}

static void prv_create_client(char * buffer,
                              void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char * end = NULL;
    int result;
    int64_t value;
    uint8_t * temp_buffer = NULL;
    int temp_length = 0;
    lwm2m_media_type_t format = LWM2M_CONTENT_TEXT;

    //Get Client ID
    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    //Get Uri
    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    //Get Data to Post
    buffer = get_next_arg(end, &end);
    if (buffer[0] == 0) goto syntax_error;

    if (!check_end_of_args(end)) goto syntax_error;

   // TLV

   /* Client dependent part   */

    if (uri.objectId == 1024)
    {
        lwm2m_data_t * dataP;

        if (1 != sscanf(buffer, "%ld", &value))
        {
            CONSOLE_LOG( "Invalid value !");
            return;
        }

        dataP = lwm2m_data_new(1);
        if (dataP == NULL)
        {
            CONSOLE_LOG( "Allocation error !");
            return;
        }
        lwm2m_data_encode_int(value, dataP);
        dataP->id = 1;

        format = LWM2M_CONTENT_TLV;
        temp_length = lwm2m_data_serialize(NULL, 1, dataP, &format, &temp_buffer);
    }
   /* End Client dependent part*/

    //Create
    result = lwm2m_dm_create(lwm2mH, clientId, &uri, format, temp_buffer, temp_length, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}

static void prv_delete_client(char * buffer,
                              void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char* end = NULL;
    int result;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_dm_delete(lwm2mH, clientId, &uri, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}

static void prv_observe_client(char * buffer,
                               void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char* end = NULL;
    int result;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_observe(lwm2mH, clientId, &uri, prv_notify_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}

static void prv_cancel_client(char * buffer,
                              void * user_data)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) user_data;
    uint16_t clientId;
    lwm2m_uri_t uri;
    char* end = NULL;
    int result;

    result = prv_read_id(buffer, &clientId);
    if (result != 1) goto syntax_error;

    buffer = get_next_arg(buffer, &end);
    if (buffer[0] == 0) goto syntax_error;

    result = lwm2m_stringToUri(buffer, end - buffer, &uri);
    if (result == 0) goto syntax_error;

    if (!check_end_of_args(end)) goto syntax_error;

    result = lwm2m_observe_cancel(lwm2mH, clientId, &uri, prv_result_callback, NULL);

    if (result == 0)
    {
        CONSOLE_LOG( "OK");
    }
    else
    {
        prv_print_error(result);
    }
    return;

syntax_error:
    CONSOLE_LOG( "Syntax error !");
}




void print_usage(void)
{
    CONSOLE_LOG( "Usage: lwm2mserver [OPTION]\r\n");
    CONSOLE_LOG( "Launch a LWM2M server on localhost.\r\n\n");
    CONSOLE_LOG( "Options:\r\n");
    CONSOLE_LOG( "  -4\t\tUse IPv4 connection. Default: IPv6 connection\r\n");
    CONSOLE_LOG( "  -l PORT\tSet the local UDP port of the Server. Default: "LWM2M_STANDARD_PORT_STR"\r\n");
    CONSOLE_LOG( "\r\n");
}



command_desc_t commands[] =
{
        {"list", "List registered clients.", NULL, prv_output_clients, NULL},
        {"read", "Read from a client.", " read CLIENT# URI\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri to read such as /3, /3/0/2, /1024/11, /1024/0/1\r\n"
                                        "Result will be displayed asynchronously.", prv_read_client, NULL},
        {"disc", "Discover resources of a client.", " disc CLIENT# URI\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri to discover such as /3, /3/0/2, /1024/11, /1024/0/1\r\n"
                                        "Result will be displayed asynchronously.", prv_discover_client, NULL},
        {"write", "Write to a client.", " write CLIENT# URI DATA\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri to write to such as /3, /3/0/2, /1024/11, /1024/0/1\r\n"
                                        "   DATA: data to write\r\n"
                                        "Result will be displayed asynchronously.", prv_write_client, NULL},
        {"time", "Write time-related attributes to a client.", " time CLIENT# URI PMIN PMAX\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri to write attributes to such as /3, /3/0/2, /1024/11, /1024/0/1\r\n"
                                        "   PMIN: Minimum period\r\n"
                                        "   PMAX: Maximum period\r\n"
                                        "Result will be displayed asynchronously.", prv_time_client, NULL},
        {"attr", "Write value-related attributes to a client.", " attr CLIENT# URI LT GT [STEP]\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri to write attributes to such as /3, /3/0/2, /1024/11, /1024/0/1\r\n"
                                        "   LT: \"Less than\" value\r\n"
                                        "   GT: \"Greater than\" value\r\n"
                                        "   STEP: \"Step\" value\r\n"
                                        "Result will be displayed asynchronously.", prv_attr_client, NULL},
        {"clear", "Clear attributes of a client.", " clear CLIENT# URI\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri to clear attributes of such as /3, /3/0/2, /1024/11, /1024/0/1\r\n"
                                        "Result will be displayed asynchronously.", prv_clear_client, NULL},
        {"exec", "Execute a client resource.", " exec CLIENT# URI\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri of the resource to execute such as /3/0/2\r\n"
                                        "Result will be displayed asynchronously.", prv_exec_client, NULL},
        {"del", "Delete a client Object instance.", " del CLIENT# URI\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri of the instance to delete such as /1024/11\r\n"
                                        "Result will be displayed asynchronously.", prv_delete_client, NULL},
        {"create", "create an Object instance.", " create CLIENT# URI DATA\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri to which create the Object Instance such as /1024, /1024/45 \r\n"
                                        "   DATA: data to initialize the new Object Instance (0-255 for object 1024) \r\n"
                                        "Result will be displayed asynchronously.", prv_create_client, NULL},
        {"observe", "Observe from a client.", " observe CLIENT# URI\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri to observe such as /3, /3/0/2, /1024/11\r\n"
                                        "Result will be displayed asynchronously.", prv_observe_client, NULL},
        {"cancel", "Cancel an observe.", " cancel CLIENT# URI\r\n"
                                        "   CLIENT#: client number as returned by command 'list'\r\n"
                                        "   URI: uri on which to cancel an observe such as /3, /3/0/2, /1024/11\r\n"
                                        "Result will be displayed asynchronously.", prv_cancel_client, NULL},

        {"q", "Quit the server.", NULL, prv_quit, NULL},

        COMMAND_END_LIST
};
