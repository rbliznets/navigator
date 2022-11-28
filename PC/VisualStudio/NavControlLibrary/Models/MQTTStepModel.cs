using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;

namespace NavControlLibrary.Models
{
    public class MQTTStepModel : NotifyModel
    {
        public delegate void eChangeGPS(MQTTStepModel script);
        public event eChangeGPS onChangeGPS;

        #region Поля
        JObject mPayload = new JObject();
        string tPayload = "";
        string mTopic = "";
        DIR_TYPES mRoute = DIR_TYPES.ANY;
        GPS_LEVEL mGPS = GPS_LEVEL.NONE;

        private bool mStop = false;
        public int mListNum = -1;
        int mListSize = 0;
        public bool mPriorLast = false;
        public bool mNoneFirst = false;
        public bool mNoneLast = false;
        public bool mPostFirst = false;
        #endregion Поля

        #region Свойства
        public bool IsNotFirst
        {
            get
            {
                switch (GPS)
                {
                    case GPS_LEVEL.PRIOR:
                        return mListNum != 1;
                    case GPS_LEVEL.NONE:
                        return !mNoneFirst;
                    default:
                        return !mPostFirst;
                }
            }
        }
        public bool IsNotLast
        {
            get
            {
                switch (GPS)
                {
                    case GPS_LEVEL.PRIOR:
                        return !mPriorLast;
                    case GPS_LEVEL.NONE:
                        return !mNoneLast;
                    default:
                        return mListNum != mListSize;
                }
            }
        }
        public int ListSize
        {
            get
            {
                return mListSize;
            }
            set
            {
                mListSize = value;
                NotifyPropertyChanged("ListSize");
                NotifyPropertyChanged("IsNotLast");
                NotifyPropertyChanged("IsNotFirst");
            }
        }
        public bool IsStop
        {
            get
            {
                return mStop;
            }
        }
        public string Info1
        {
            get
            {
                string res = "";
                if ((!IsStop) && (Route != DIR_TYPES.ANY)) res += Route.ToString();
                if (GPS != GPS_LEVEL.NONE)
                {
                    if (res != "") res += ",";
                    res += GPS.ToString();
                }
                return res.Trim();
            }
        }
        public string Payload
        {
            get
            {
                if (GetErrors("Payload") == null)
                {
                    return mPayload.ToString();
                }
                else return tPayload;
            }
            set
            {
                tPayload = value;
                try
                {
                    mPayload = JObject.Parse(value);
                    ClearError("Payload");
                    NotifyPropertyChanged(nameof(Payload));
                }
                catch
                {
                    SetError("Payload", "Должно быть JSON.");
                }
            }

        }
        public string PayloadLine
        {
            get
            {
                return mPayload.ToString(Newtonsoft.Json.Formatting.None);
            }
        }

        public string Topic
        {
            get
            {
                return mTopic;
            }
            set
            {
                mTopic = value;
                NotifyPropertyChanged(nameof(Topic));
            }
        }
        public DIR_TYPES Route
        {
            get
            {
                return mRoute;
            }
            set
            {
                mRoute = value;
                NotifyPropertyChanged(nameof(Route));
                NotifyPropertyChanged(nameof(Info1));
            }
        }
        public List<DIR_TYPES> DirTypeList
        {
            get
            {
                return Enum.GetValues<DIR_TYPES>().ToList();
            }
        }

        public GPS_LEVEL GPS
        {
            get
            {
                return mGPS;
            }
            set
            {
                mGPS = value;
                NotifyPropertyChanged(nameof(GPS));
                NotifyPropertyChanged(nameof(Info1));
                if (onChangeGPS != null) onChangeGPS(this);
            }
        }
        public List<GPS_LEVEL> GPSList
        {
            get
            {
                return Enum.GetValues<GPS_LEVEL>().ToList();
            }
        }
        #endregion Свойства

        public MQTTStepModel(JToken json, DIR_TYPES? route)
        {
            if (json["topic"] != null) Topic = (string)json["topic"];
            if (json["payload"] != null) Payload = json["payload"].ToString();
            if (json["gps"] != null)
            {
                switch ((string)json["gps"])
                {
                    case "prior":
                        GPS = GPS_LEVEL.PRIOR;
                        break;
                    case "post":
                        GPS = GPS_LEVEL.POST;
                        break;
                }
            }
            if ((route == null) || (route == DIR_TYPES.ANY))
            {
                if (json["route"] != null)
                {
                    switch ((string)json["route"])
                    {
                        case "forward":
                            Route = DIR_TYPES.FORWARD;
                            break;
                        case "backward":
                            Route = DIR_TYPES.BACKWARD;
                            break;
                    }
                }
            }
            else
            {
                Route = (DIR_TYPES)route;
                mStop = true;
            }
        }
        public MQTTStepModel(DIR_TYPES? route)
        {
            if (route != null)
            {
                Route = (DIR_TYPES)route;
                mStop = true;
            }
        }
        public MQTTStepModel(MQTTStepModel step)
        {
            Route = step.Route;
            mStop = step.mStop;
            GPS = step.GPS;
        }

        internal JToken GetJToken(bool isNonStop)
        {
            JObject res = new JObject();
            res["topic"] = Topic;
            res["payload"] = mPayload;
            switch (GPS)
            {
                case GPS_LEVEL.PRIOR:
                    res["gps"] = "prior";
                    break;
                case GPS_LEVEL.POST:
                    res["gps"] = "post";
                    break;
            }
            if (isNonStop)
            {
                switch (Route)
                {
                    case DIR_TYPES.FORWARD:
                        res["route"] = "forward";
                        break;
                    case DIR_TYPES.BACKWARD:
                        res["route"] = "backward";
                        break;
                }
            }
            return res;
        }

        public override string ToString()
        {
            string str = "{" + Topic + ":" + Payload;
            if (GPS != GPS_LEVEL.NONE) str += ";" + GPS.ToString();
            if (Route != DIR_TYPES.ANY) str += ";" + Route.ToString();
            str += "}";
            return str;
        }
    }
}
