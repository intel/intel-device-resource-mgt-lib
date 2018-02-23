/*
 * path_util.c
 *
 */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE         /* See feature_test_macros(7) */
#endif
#include <stdio.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <fcntl.h>           /* Definition of AT_* constants */
#include <unistd.h>
#include <string.h>
#include <unistd.h>
#include <libgen.h>

/*
 * http://stackoverflow.com/questions/606041/how-do-i-get-the-path-of-a-process-in-unix-linux?rq=1
 */
char * getExecPath (char * path,size_t dest_len, char * argv0)
{
    char * baseName = NULL;
    char * systemPath = NULL;
    char * candidateDir = NULL;

    /* the easiest case: we are in linux */
    memset((void*)path, (int)0, dest_len);
    if (readlink ("/proc/self/exe", path, dest_len) != -1)
    {
        dirname (path);
        strcat  (path, "/");
        return path;
    }

    /* Ups... not in linux, no  guarantee */

    /* check if we have something like execve("foobar", NULL, NULL) */
    if (argv0 == NULL)
    {
        /* we surrender and give current path instead */
        if (getcwd (path, dest_len) == NULL) return NULL;
        strcat  (path, "/");
        return path;
    }


    /* argv[0] */
    /* if dest_len < PATH_MAX may cause buffer overflow */
    if ((realpath (argv0, path)) && (!access (path, F_OK)))
    {
        dirname (path);
        strcat  (path, "/");
        return path;
    }

    /* Current path */
    baseName = basename (argv0);
    if (getcwd (path, dest_len - strlen (baseName) - 1) == NULL)
        return NULL;

    strcat (path, "/");
    strcat (path, baseName);
    if (access (path, F_OK) == 0)
    {
        dirname (path);
        strcat  (path, "/");
        return path;
    }

    /* Try the PATH. */
    systemPath = getenv ("PATH");
    if (systemPath != NULL)
    {
        dest_len--;
        systemPath = strdup (systemPath);
        for (candidateDir = strtok (systemPath, ":"); candidateDir != NULL; candidateDir = strtok (NULL, ":"))
        {
            strncpy (path, candidateDir, dest_len);
            strncat (path, "/", dest_len);
            strncat (path, baseName, dest_len);

            if (access(path, F_OK) == 0)
            {
                free (systemPath);
                dirname (path);
                strcat  (path, "/");
                return path;
            }
        }
        free(systemPath);
        dest_len++;
    }

    /* again someone has use execve: we dont knowe the executable name; we surrender and give instead current path */
    if (getcwd (path, dest_len - 1) == NULL) return NULL;
    strcat  (path, "/");
    return path;
}


//该方法也能让 so 文件换取自身的位置，方便插件的编写。
char * get_module_path(void* address)
{
    if (! address) {
        address = (void*)(&get_module_path);
    }

    Dl_info dl_info;
    dl_info.dli_fname = 0;
    int const ret = dladdr(address, &dl_info);
    if (0 != ret && dl_info.dli_fname != NULL)
    {
        return strdup(dl_info.dli_fname);
    }
    return NULL;
}

