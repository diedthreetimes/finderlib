#pragma once

namespace psicrypto{

template <typename ELEMENT, typename EXP>
class AbstractGroup
{
public:
        typedef ELEMENT element_type;
        typedef EXP exp_type;

        const ELEMENT default_element;
        
        AbstractGroup(const ELEMENT &default_element) 
                : default_element(default_element)
        {
        }        
};

};
