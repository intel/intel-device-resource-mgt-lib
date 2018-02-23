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


#ifndef CRESVALUE_H_
#define CRESVALUE_H_
#include <stdlib.h>
#include <string>

typedef enum _value_t
{
	T_None = 0,
	T_String = 1,
	T_Bool =2,
	T_Int =3 ,
	T_Float =4,
	T_DateTime = 5,
	T_Binary  =6,
	T_Alert = 7,
	T_Photo = 8,
	T_Max_Value
} VALUE_T;

enum {
	R_Equal = 1,
	R_Not_Equal = 2,
	R_Smaller = 3,
	R_Greater = 4
};

#ifndef FLOAT_EQ
const double  MY_DBL_EPSILON = 2.2204460492503131e-016;
#define FLOAT_EQ(x,v) (((v - MY_DBL_EPSILON) < x) && (x <( v + MY_DBL_EPSILON)))
#endif

class CResValue {
public:
	CResValue();
	virtual ~CResValue();



    CResValue& operator = (const CResValue & res);

    void Clean();
    VALUE_T GetType() { return m_data_type;};
    bool HasValue(){ return m_data_type != T_None;};


    void SetInt(int);
    void SetDateTime(time_t);
    void SetFloat(float );
    void SetString(const char *);
    void SetBool(bool);
    void SetAlert(int);

    bool SetValue(VALUE_T t, const char * s);
    bool GetValue(std::string& res);

    int GetInt() const;
    time_t GetDateTime() const;
    float GetFloat() const;
    char * GetString() const;
    bool GetBool() const;
    int GetAlert() const;

    bool CompareCondition(int compaire_type, const char *);
    bool Compare(int compaire_type, CResValue &);

    char * dump(char * buffer, int size);

private:
    VALUE_T m_data_type;
    union
    {
        int iVal;
        char * sVal;
        time_t tVal;
        float fVal;
        bool bVal;
    }m_val;

    // only valid for T_Binary data type
    int m_data_len;
};

#endif /* CRESVALUE_H_ */
