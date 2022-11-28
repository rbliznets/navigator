using Newtonsoft.Json.Linq;
using Quartz;
using System.ComponentModel;
using System.Linq;

namespace NavControlLibrary.Models
{
    public class CronTime : NotifyModel
    {
        string mSchedule = "0 * * * * ? *";
        string tSchedule = "";

        public string Schedule
        {
            get
            {
                if (GetErrors("Schedule") == null)
                {
                    return mSchedule;
                }
                else return tSchedule;
            }
            set
            {
                tSchedule = value;
                if (CronExpression.IsValidExpression(value))
                {
                    ClearError("Schedule");
                    mSchedule = value;
                    NotifyPropertyChanged(nameof(Schedule));
                    NotifyPropertyChanged(nameof(Description));
                }
                else
                {
                    SetError("Schedule", "Проверьте формат");
                }
            }
        }
        public string Description
        {
            get
            {
                return CronExpressionDescriptor.ExpressionDescriptor.GetDescription(mSchedule);
            }
        }

        public CronTime(string str)
        {
            Schedule = str;
        }

    }
    public class TimeTriggerModel : NotifyModel
    {
        public BindingList<CronTime> Cronlike
        {
            get;
        }
        public TimeTriggerModel(JToken json)
        {
            Cronlike = new BindingList<CronTime>();

            if (json["cronlike"] != null)
            {
                foreach (var itm in json["cronlike"].Children())
                {
                    string str = (string)itm;
                    if ((str == "") || (CronExpression.IsValidExpression(str)))
                    {
                        Cronlike.Add(new CronTime(str));
                    }
                }
            }
        }

        internal JToken GetJToken()
        {
            JObject res = new JObject();
            JArray cronlike = new JArray();
            var lst = Cronlike.ToList().Select(x => x.Schedule).Distinct();
            foreach (var str in lst) cronlike.Add(str);
            res["cronlike"] = cronlike;
            return res;
        }

        public TimeTriggerModel()
        {
            Cronlike = new BindingList<CronTime>();
        }

        public override string ToString()
        {
            string str = "time{";
            foreach (var itm in Cronlike)
            {
                if (itm.Schedule == "") str += "once";
                else
                {
                    str += itm.Description;
                }
                if (itm != Cronlike.Last()) str += ";";
            }
            str += "}";
            return str;
        }
    }
}
