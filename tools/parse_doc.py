#! env python3

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

from html.parser import HTMLParser
from os import listdir, path
from getopt import getopt, GetoptError
from sys import argv
from glob import glob

REFERENCES_FIELD = ("required", "overrides", "defaults", "libs", "export_generated_headers", "export_header_lib_headers", "export_shared_lib_headers", "export_static_lib_headers", "generated_headers", "generated_sources")

class TextWriter:
    def __init__(self) -> None:
        self.ident = ''

    def start_blueprint(self, name):
        print("%s {" % name)
        self.ident += "    "

    def end_blueprint(self):
        self.ident = ''
        print("}")

    def field(self, name, type, desc):
        print("%s%s (%s;%s)" % (self.ident, name, type, desc))

    def start_fields_list(self, name, type, desc):
        print("%s%s (%s;%s) {" % (self.ident, name, type, desc))
        self.ident += "    "

    def end_fields_list(self):
        self.ident = self.ident[:-4]
        print("%s}" % self.ident)

class XmlWriter:
    def __init__(self, out) -> None:
        self.ident_ = ""
        self.out_ = out

    def start(self):
        self.out_.write("<blueprints>\n")
        self.ident_ += "    "
    
    def end(self):
        self.out_.write("</blueprints>\n")
        self.ident_ = self.ident_[:-4]

    def start_blueprint(self, name):
        self.out_.write("%s<blueprint name=\"%s\">\n" % (self.ident_, name))
        self.ident_ += "    "

    def end_blueprint(self):
        self.ident_ = self.ident_[:-4]
        self.out_.write("%s</blueprint>\n" % self.ident_)

    def field(self, name, type, desc):
        self.out_.write("%s<field %s/>\n" % (self.ident_, self.format_field_(name, type, desc)))

    def start_fields_list(self, name, type, desc):
        self.out_.write("%s<object-field %s>\n" % (self.ident_, self.format_field_(name, type, desc)))
        self.ident_ += "    "

    def end_fields_list(self):
        self.ident_ = self.ident_[:-4]
        self.out_.write("%s</object-field>\n" % self.ident_)

    def format_field_(self, name, type, desc):
        text = "name=\"%s\"" % name
        if type != None:
            text += " type=\"%s\"" % self.convert_type_(name, type)
        if desc != None:
            text += " descr=\"%s\"" % desc.replace("\"", "&quot;").replace("\n", "\\n")
        return text

    def convert_type_(self, name, type):
        if type == None:
            return None
        elif type == "string":
            return "string"
        elif type == "list of strings":
            if name in REFERENCES_FIELD:
                return "blueprint[]"
            if name.endswith("_libs"):
                return "blueprint[]"
            return "string[]"
        elif type == "bool":
            return "bool"
        elif type == "StaticSharedLibraryProperties": # What is actualy it?
            return "string[]"
        elif type == "TestOptions": # What is actualy it?
            return "string[]"
        elif type == "apexMultilibProperties":
            return "string[]"
        elif type == "int64":
            return "number"
        elif type == "codegenArchProperties": # Should we define a type for them?
            return "string[]"
        elif type == "ApiToCheck":
            return "string"
        elif type == "VersionProperties":
            return "string[]"

        raise ValueError("Not supported type: %s field: %s" % (type, name))

class BlueprintField:
    def __init__(self, tag):
        self.name = tag["name"] if "name" in tag else None
        self.type = tag["type"] if "type" in tag else None
        if "desc" in tag:
            desc = tag["desc"]
            self.desc = desc[2:] if desc.startswith(", ") else desc                
        else:
            self.desc = None
        self.tag_ = tag
        self.is_auto_added = False
        self.fields = None
        pass

    def add(self, field_info):
        if self.fields == None:
            self.fields = list()
        self.fields.append(field_info)

    def last(self):
        size = len(self.fields)
        if size == 0:
            return None
        return self.fields[size - 1]
    
    def is_container(self):
        return True if "class" in self.tag_ and self.tag_["class"] == "accordion" else False

    def field(self, name):
        if self.fields == None:
            return None
        
        for field in self.fields:
            if field.name == name:
                return field

        return None

    def print(self, printer):
        if self.fields != None:
            printer.start_fields_list(self.name, self.type, self.desc)
            for field in self.fields:
                field.print(printer)
            printer.end_fields_list()
        else:
            printer.field(self.name, self.type, self.desc)

class Blueprint:
    def __init__(self, name):
        self.name = name
        self.is_auto_added = False
        self.fields = list()

    def add(self, field_info):
        self.fields.append(field_info)

    def last(self):
        size = len(self.fields)
        if size == 0:
            return None
        return self.fields[size - 1]

    def field(self, name):
        if self.field == None:
            return None
        for field in self.fields:
            if field.name == name:
                return field
        return None

    def print(self, printer):
        printer.start_blueprint(self.name)
        if self.fields != None:
            for field in self.fields:
                field.print(printer)
        printer.end_blueprint()


class SoongDocParser(HTMLParser):
    def __init__(self):
        self.stack_ = list()
        self.is_item_ = False
        self.blueprints = list()
        self.objects_stack_ = list()
        HTMLParser.__init__(self)

    def handle_starttag(self, tag, attrs):
        if self.is_ignored_(tag):
            return

        info = dict()
        info["tag"] = tag
        for attr in attrs:
            if attr[0] == "class":
                info["class"] = attr[1]
            elif attr[0] == "id":
                info["id"] = attr[1]

        if tag == "div":
            if "id" in info:
                self.is_item_ = True
            elif "class" in info and info["class"] == "collapsible":
                current = self.current_container_()
                if current == None:
                    raise IOError("Broken fields tree")
                last = current.last()
                if last != None and last.is_container():
                    self.objects_stack_.append(last)

        self.stack_.append(info)
    
    def handle_endtag(self, tag):
        if len(self.stack_) == 0:
            raise IOError("Unexpected tag",tag,"end")
        
        current = self.stack_.pop()
        if current["tag"] != tag:
            raise IOError("Unexpected tag %s end. Current open tag is %s" % (tag ,current["tag"]))
        
        if current["tag"] == "div":
            if "id" in current:
                self.is_item_ = False
                container = self.current_container_()

                name = current["name"] if "name" in current else None
                if current == None:
                    raise IOError("No field name")


                if container != None:
                    point_index = name.find(".")
                    if point_index != -1:
                        for sub_name in name.split('.'):
                            if container.name == sub_name:
                                continue

                            field = container.field(sub_name)
                            if field == None:
                                info = { "name" : sub_name, "tag" : "div", "class" : current["class"] }
                                field = BlueprintField(info)
                                container.add(field)
                            container = field
                            container.is_auto_added = True
                            self.objects_stack_.append(container)
                        # An ugly hack
                        self.objects_stack_.pop()
                        # print(' '.join(str(item) for item in self.objects_stack_))
                    else:
                        field = BlueprintField(current)
                        container.add(field)
            elif "class" in current and current["class"] == "collapsible":
                items_poped = 0
                while True:
                    last = self.objects_stack_.pop()
                    items_poped += 1
                    if last.is_auto_added != True:
                        if items_poped > 1:
                            self.objects_stack_.append(last)
                        break
    
    def handle_data(self, data):
        current = self.current_tag_()
        if current == None:
            return

        text = data.strip()
        if len(text) == 0:
            return            

        if current["tag"] == "h2":
            blueprint = Blueprint(text)
            self.blueprints.append(blueprint)
            self.objects_stack_.append(blueprint)
            return

        if current["tag"] == "i" and self.is_item_:
            tag = self.current_field_()
            if tag != None and not "type" in tag:
                tag["type"] = text
            return
        
        if current["tag"] == "b" and self.is_item_:
            tag = self.current_field_()
            if tag != None:
                tag["name"] = text
            return

        if current["tag"] == "div" and "id" in current:
            if "desc" in current:
                current["desc"] += " "
                current["desc"] += text
            else:
                current["desc"] = text
            return
        

    def is_ignored_(self, tag):
        if tag == "link" or tag == "p":
            return True
        
        return False

    def current_tag_(self):
        size = len(self.stack_)
        if (size == 0):
            return None
        return self.stack_[size - 1]

    def current_field_(self):
        for item in reversed(self.stack_):
            if item["tag"] == "div" and "id" in item:
                return item
        return None

    def current_container_(self):
        size = len(self.objects_stack_)
        if (size == 0):
            return None
        return self.objects_stack_[size - 1]

def main(argv):
    try:
        opts, _ = getopt(argv, "", ["in=", "out="])
        input_dir = None
        out_file = None
        for opt, arg in opts:
            if opt == "--in":
                input_dir = arg
            elif opt == "--out":
                out_file = arg
        
        if input_dir == None:
            print("Input folder is not specified")
            return -1

        if out_file == None:
            print("Output file is not specified")
            return -1

        if not path.exists(input_dir):
            print("Input folder %s does not exist" % input_dir)
            return -1

        if path.isdir(out_file):
            print("There is a folder with the outputr file name %s" % out_file)
            return -1

        out_dir = path.dirname(out_file)
        if not path.exists(out_dir):
            print("Output folder %s does not exist" % out_dir)
            return -1

        with open(out_file, "w") as out:
            printer = XmlWriter(out)
            printer.start()
            for file_name in glob("%s/*.html" % input_dir):
                print("Reading %s..." % file_name)
                parser = SoongDocParser()
                with open(file_name) as text:
                    for line in text:
                        parser.feed(line)

                for blueprint in parser.blueprints:
                    blueprint.print(printer)
            printer.end()


    except GetoptError as e:
        print(e.msg)

if __name__ == "__main__":
    main(argv[1:])
