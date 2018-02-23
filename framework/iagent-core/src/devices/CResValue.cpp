/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#include "CResValue.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "logs.h"
#include "iagent_base.h"

CResValue::CResValue():
m_data_type(T_None),
m_data_len(0)
{
    // TODO Auto-generated constructor stub
}


CResValue::~CResValue()
{

    Clean();
}


char * CResValue::dump(char * buffer, int size)
{
    if (m_data_type == T_String)
    {
        snprintf(buffer, size, "string: %s", this->GetString());
    }
    else if (m_data_type == T_DateTime)
    {
        snprintf(buffer, size, "date: %ld", this->GetDateTime());
    }
    else if (m_data_type == T_Int)
    {
        snprintf(buffer, size, "int: %d", this->GetInt());
    }
    else if (m_data_type == T_Float)
    {
        snprintf(buffer, size, "float: %.2f", this->GetFloat());
    }
    else if (m_data_type == T_Alert)
    {
        snprintf(buffer, size, "%d", this->GetAlert());
    }
    else if (m_data_type == T_Bool)
    {
        snprintf(buffer, size, "bool: %d", this->GetBool()?1:0);
    }
    else
    {
        snprintf(buffer, size, "unknow type %d", m_data_type);
    }

    return buffer;


}

CResValue& CResValue::operator = (const CResValue & res)
{
    if (this!=&res)
    {
        if (res.m_data_type == T_String)
        {
            SetString(res.GetString());
        }
        else if (res.m_data_type == T_DateTime)
        {
            SetDateTime(res.GetDateTime());
        }
        else if (res.m_data_type == T_Int)
        {
            SetInt(res.GetInt());
        }
        else if (res.m_data_type == T_Float)
        {
            SetFloat(res.GetFloat());
        }
        else if (res.m_data_type == T_Alert)
        {
            SetAlert(res.GetAlert());
        }
        else if (res.m_data_type == T_Bool)
        {
            SetBool(res.GetBool());
        }
        else
        {
            LOG_MSG("CResValue did not support this type")
        }
    }

    return *this;
}


void CResValue::Clean()
{
    if (m_data_type == T_Binary || m_data_type == T_String)
    {
        if (m_val.sVal )
        {
            m_data_len = 0;
            free(m_val.sVal);
            m_data_type = T_None;
            m_val.sVal = NULL;
        }
    }
}


void CResValue::SetInt(int val)
{
    Clean();
    m_data_type = T_Int;
    m_val.iVal = val;
}


void CResValue::SetDateTime(time_t t)
{
    Clean();
    m_data_type = T_DateTime;
    m_val.tVal = t;
}


void CResValue::SetFloat(float f)
{
    Clean();
    m_data_type = T_Float;
    m_val.fVal = f;
}


void CResValue::SetString(const char *s)
{
    Clean();
    int len = strlen(s)+1;
    m_val.sVal = (char*) malloc(len);

    if(m_val.sVal == NULL)
    {
        ERROR ("allocate memory failed for SetString");
        return;
    }

    m_data_len = len;
    strcpy (m_val.sVal, s);
    m_data_type = T_String;
}


void CResValue::SetBool(bool b)
{
    Clean();
    m_data_type = T_Bool;
    m_val.bVal = b;
}


void CResValue::SetAlert(int a)
{
    Clean();
    m_data_type = T_Alert;
    m_val.iVal = a;
}


int CResValue::GetInt() const
{
    if (m_data_type == T_Int)
        return m_val.iVal;

    throw(1);
}


time_t CResValue::GetDateTime() const
{
    if(m_data_type == T_DateTime)
        return m_val.tVal;

    throw(2);

}

float CResValue::GetFloat() const
{
    if(m_data_type == T_Float)
        return m_val.fVal;

    throw(3);
}


char *CResValue::GetString() const
{
    if(m_data_type == T_String)
        return m_val.sVal;

    throw(4);
}


bool CResValue::GetBool() const
{
    if(m_data_type == T_Bool)
        return m_val.bVal;

    throw(5);
}


int CResValue::GetAlert() const
{
    if(m_data_type == T_Alert)
        return m_val.iVal;

    throw(6);
}


// This function is called by REST thread, it should be protected
// by the rw lock.
bool CResValue::GetValue(std::string& value)
{
    if (m_data_type == T_String)
    {
        value = this->GetString();
    }
    else if (m_data_type == T_DateTime)
    {
        char c[100];
        sprintf(c, "%ld", this->GetDateTime());
        value = c;
    }
    else if (m_data_type == T_Int)
    {
        char c[100] = {0};
        sprintf(c, "%d", this->GetInt());
        value = c;
    }
    else if (m_data_type == T_Float)
    {
        char c[100];
        sprintf(c, "%.2f", this->GetFloat());
        value = c;
    }
    else if (m_data_type == T_Alert)
    {
        char c[100];
        sprintf(c, "%d", this->GetAlert());
        value = c;
    }
    else if (m_data_type == T_Bool)
    {
        char c[100];
        sprintf(c, "%d", this->GetBool()?1:0);
        value = c;
    }
    else
    {
        value = "";
        LOG_RETURN (false)
    }

    return true;
}


// set value from string for a predefined data type
bool CResValue::SetValue(VALUE_T t, const char *s)
{
    if (t == T_String)
    {
        SetString(s);
    }
    else if (t == T_DateTime)
    {
        SetDateTime((time_t)atoi(s));
    }
    else if (t == T_Int)
    {
        SetInt(atoi(s));
    }
    else if (t == T_Float)
    {
        SetFloat(atof(s));
    }
    else if (t == T_Alert)
    {
        SetAlert(atoi(s));
    }
    else if (t == T_Bool)
    {
        int i = atoi(s);
        SetBool(i!=0);
    }
    else
    {
        LOG_RETURN(false)
    }

    return true;
}


bool CResValue::Compare(int compaire_type, CResValue & target)
{
    bool result = false;

    if (target.GetType() != GetType())
        LOG_RETURN (false)

    if (m_data_type == T_String)
    {
        switch (compaire_type)
        {
        case R_Equal:
            return (strcmp(GetString(), target.GetString()) == 0);
        case R_Not_Equal:
            return (strcmp(GetString(), target.GetString()) != 0);
        case R_Greater:
            result = (strcmp(GetString(), target.GetString()) > 0);
            return result;
        case R_Smaller:
            result = (strcmp(GetString(), target.GetString()) < 0);
            return result;
        default:
            LOG_RETURN (false)
        }
    }
    else if (m_data_type == T_DateTime)
    {
        switch (compaire_type)
        {
        case R_Equal:
            return (target.GetDateTime() == GetDateTime());
        case R_Not_Equal:
            return (target.GetDateTime() != GetDateTime());
        case R_Greater:
            return (target.GetDateTime() < this->GetDateTime());
        case R_Smaller:
            return (target.GetDateTime() > this->GetDateTime());
        default:
            LOG_RETURN (false)
        }
    }
    else if (m_data_type == T_Int)
    {
        switch (compaire_type)
        {
        case R_Equal:
            return (target.GetInt() == this->GetInt());
        case R_Not_Equal:
            return (target.GetInt() != this->GetInt());
        case R_Greater:
            return (target.GetInt() < this->GetInt());
        case R_Smaller:
            return (target.GetInt() > this->GetInt());
        default:
            LOG_RETURN (false)
        }
    }
    else if (m_data_type == T_Float)
    {
        switch (compaire_type)
        {
        case R_Equal:
            return (FLOAT_EQ(target.GetFloat() , this->GetFloat()));
        case R_Not_Equal:
            return (!FLOAT_EQ(target.GetFloat(), this->GetFloat()));
        case R_Greater:
            return (target.GetFloat() < this->GetFloat());
        case R_Smaller:
            return (target.GetFloat() > this->GetFloat());
        default:
            LOG_RETURN (false)
        }
    }
    else if (m_data_type == T_Alert)
    {
        switch (compaire_type)
        {
        case R_Equal:
            return (target.GetAlert() == this->GetAlert());
        case R_Not_Equal:
            return (target.GetAlert() != this->GetAlert());
        case R_Greater:
            return (target.GetAlert() < this->GetAlert());
        case R_Smaller:
            return (target.GetAlert() > this->GetAlert());
        default:
            LOG_RETURN (false)
        }
    }
    else if (m_data_type == T_Bool)
    {
        switch (compaire_type)
        {
        case R_Equal:
            return (target.GetBool() && this->GetBool());
        case R_Not_Equal:
            return (target.GetBool() ^ this->GetBool());
        case R_Greater:
            return ((!target.GetBool()) && this->GetBool());
        case R_Smaller:
            return (target.GetBool() && (!this->GetBool()));
        default:
            LOG_RETURN (false)
        }
    }

    LOG_RETURN (false)
}


bool CResValue::CompareCondition(int compaire_type, const char *s)
{
    CResValue target;

    target.SetValue(m_data_type, s);

    return Compare(compaire_type, target);
}
